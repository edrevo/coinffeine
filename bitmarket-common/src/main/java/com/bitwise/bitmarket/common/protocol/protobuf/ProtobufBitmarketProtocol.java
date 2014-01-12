package com.bitwise.bitmarket.common.protocol.protobuf;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Future;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.googlecode.protobuf.pro.duplex.PeerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bitwise.bitmarket.common.PeerConnection;
import com.bitwise.bitmarket.common.protocol.*;
import com.bitwise.bitmarket.common.protorpc.PeerServer;
import com.bitwise.bitmarket.common.protorpc.PeerSession;

public class ProtobufBitmarketProtocol implements BitmarketProtocol, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtobufBitmarketProtocol.class);

    private final PeerServer peerServer;
    private final PeerSession broadcastRemoteSession;
    private final BitmarketProtobuf.BroadcastService.Interface broadcastRemote;

    private OfferListener offerListener;
    private ExchangeRequestListener exchangeRequestListener;

    public ProtobufBitmarketProtocol(PeerConnection broadcastServer)
            throws BitmarketProtocolException, InterruptedException {
        this(broadcastServer, defaultLocalConnection());
    }

    public ProtobufBitmarketProtocol(
            PeerConnection broadcastServer,
            PeerConnection localConnection)
            throws BitmarketProtocolException, InterruptedException {
        this.peerServer = this.createPeerServer(localConnection);
        this.broadcastRemoteSession = this.createRemoteSession(broadcastServer);
        this.broadcastRemote = this.createBroadcastRemote(this.broadcastRemoteSession);
        this.offerListener = this.createDefaultOfferListener();
        this.exchangeRequestListener = this.createDefaultOfferAcceptedListener();

        this.peerServer.start().await();
    }

    @Override
    public void close() throws Exception {
        this.peerServer.shutdown();
    }

    @Override
    public void setOfferListener(OfferListener listener) {
        this.offerListener = listener;
    }

    @Override
    public void setExchangeRequestListener(ExchangeRequestListener listener) {
        this.exchangeRequestListener = listener;
    }

    @Override
    public Future<Void> publish(Offer offer) {
        PublishResponseCallback callback = new PublishResponseCallback();
        this.broadcastRemote.publish(
                this.broadcastRemoteSession.getController(),
                ProtobufConversions.toProtobuf(offer),
                callback);
        return callback.getFuture();
    }

    @Override
    public Future<Void> requestExchange(
            com.bitwise.bitmarket.common.protocol.ExchangeRequest acceptance,
            PeerConnection recipient) throws BitmarketProtocolException {
        try {
            PeerSession session = this.peerServer.peerWith(peerInfo(recipient));
            BitmarketProtobuf.PeerService.Interface rcpt =
                    BitmarketProtobuf.PeerService.newStub(session.getChannel());
            RequestExchangeCallback callback = new RequestExchangeCallback();
            rcpt.requestExchange(
                    session.getController(),
                    ProtobufConversions.toProtobuf(acceptance),
                    callback);
            return callback.getFuture();
        } catch (IOException e) {
            throw new BitmarketProtocolException(
                    String.format("IO error while accepting offer %s", acceptance.toString()), e);
        }
    }

    private PeerServer createPeerServer(PeerConnection localConnection) {
        return new PeerServer(
                peerInfo(localConnection),
                BitmarketProtobuf.PeerService.newReflectiveService(new PeerServiceImpl()));
    }

    private PeerSession createRemoteSession(
            PeerConnection remotePeer) throws BitmarketProtocolException {
        try {
            return this.peerServer.peerWith(peerInfo(remotePeer));
        } catch (IOException e) {
            throw new BitmarketProtocolException(
                    String.format(
                            "cannot create session to remote peer %s", remotePeer.toString()),
                    e);
        }
    }

    private BitmarketProtobuf.BroadcastService.Interface createBroadcastRemote(
            PeerSession session) {
        return BitmarketProtobuf.BroadcastService.newStub(session.getChannel());
    }

    private OfferListener createDefaultOfferListener() {
        return new OfferListener() {
            @Override
            public void onOffer(Offer offer) {
                LOGGER.warn("incoming offer discarded due to unset listener: %s", offer.toString());
            }
        };
    }

    private ExchangeRequestListener createDefaultOfferAcceptedListener() {
        return new ExchangeRequestListener() {
            @Override
            public void onExchangeRequest(
                    com.bitwise.bitmarket.common.protocol.ExchangeRequest acceptance) {
                LOGGER.warn(
                        "incoming offer acceptance discarded due to unset listener: %s",
                        acceptance.toString());
            }
        };
    }

    private static PeerInfo peerInfo(PeerConnection conn) {
        return new PeerInfo(conn.hostname(), conn.port());
    }

    private static PeerConnection defaultLocalConnection() throws BitmarketProtocolException {
        try {
            return new PeerConnection(
                    InetAddress.getLocalHost().getHostAddress(), PeerConnection.DefaultPort());
        } catch (UnknownHostException e) {
            throw new BitmarketProtocolException(
                    "cannot obtain hostname for local machine", e);
        }
    }

    private static final BitmarketProtobuf.PublishResponse successPublishResponse =
            BitmarketProtobuf.PublishResponse.newBuilder()
                    .setResult(BitmarketProtobuf.PublishResponse.Result.SUCCESS)
                    .build();

    private static final BitmarketProtobuf.PublishResponse unavailableServicePublishResponse =
            BitmarketProtobuf.PublishResponse.newBuilder()
                    .setResult(BitmarketProtobuf.PublishResponse.Result.SERVICE_UNAVAILABLE)
                    .build();

    private static final BitmarketProtobuf.ExchangeRequestResponse successExchangeRequestResponse =
            BitmarketProtobuf.ExchangeRequestResponse.newBuilder()
                    .setResult(BitmarketProtobuf.ExchangeRequestResponse.Result.SUCCESS)
                    .build();

    private static final BitmarketProtobuf.ExchangeRequestResponse invalidAmountExchangeRequestResponse =
            BitmarketProtobuf.ExchangeRequestResponse.newBuilder()
                    .setResult(BitmarketProtobuf.ExchangeRequestResponse.Result.INVALID_AMOUNT)
                    .build();

    private class PeerServiceImpl implements BitmarketProtobuf.PeerService.Interface {

        @Override
        public void notifyMatch(
                RpcController controller,
                BitmarketProtobuf.OrderMatch request,
                RpcCallback<BitmarketProtobuf.Void> done) {
            // FIXME: do nothing for the moment
            done.run(BitmarketProtobuf.Void.getDefaultInstance());
        }

        @Override
        public void publish(
                RpcController controller,
                BitmarketProtobuf.Offer offer,
                RpcCallback<BitmarketProtobuf.PublishResponse> done) {
            try {
                ProtobufBitmarketProtocol.this.offerListener
                        .onOffer(ProtobufConversions.fromProtobuf(offer));
                done.run(successPublishResponse);
            } catch (Throwable e) {
                done.run(unavailableServicePublishResponse);
            }
        }

        @Override
        public void requestExchange(
                RpcController controller,
                BitmarketProtobuf.ExchangeRequest request,
                RpcCallback<BitmarketProtobuf.ExchangeRequestResponse> done) {
            try {
                ProtobufBitmarketProtocol.this.exchangeRequestListener
                        .onExchangeRequest(ProtobufConversions.fromProtobuf(request));
                done.run(successExchangeRequestResponse);
            } catch (ExchangeRejectedException e) {
                done.run(invalidAmountExchangeRequestResponse);
            }
        }
    }

    private class PublishResponseCallback extends RpcCallbackFutureMapper<
            BitmarketProtobuf.PublishResponse, Void> {

        @Override
        protected Void processResponse(BitmarketProtobuf.PublishResponse response)
                throws Exception {
            switch (response.getResult().getNumber()) {
                case BitmarketProtobuf.PublishResponse.Result.SERVICE_UNAVAILABLE_VALUE:
                    throw new OfferRejectedException(
                            OfferRejectedException.Reason.BROADCAST_FAILED);
            }
            return null;
        }
    }

    private class RequestExchangeCallback extends RpcCallbackFutureMapper<
            BitmarketProtobuf.ExchangeRequestResponse, Void> {

        @Override
        protected Void processResponse(
                BitmarketProtobuf.ExchangeRequestResponse result) throws Exception {
            switch (result.getResult().getNumber()) {
                case BitmarketProtobuf.ExchangeRequestResponse.Result.INVALID_AMOUNT_VALUE:
                    throw new ExchangeRejectedException(
                            ExchangeRejectedException.Reason.INVALID_AMOUNT);
            }
            return null;
        }
    }
}
