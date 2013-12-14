package com.bitwise.bitmarket.common.protocol.protobuf;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.bitwise.bitmarket.common.protocol.ExchangeRejectedException;
import com.bitwise.bitmarket.common.PeerConnection;
import com.bitwise.bitmarket.common.protocol.*;
import com.bitwise.bitmarket.common.protorpc.PeerServer;
import com.bitwise.bitmarket.common.protorpc.PeerSession;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;
import com.googlecode.protobuf.pro.duplex.PeerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtobufBitmarketProtocol implements BitmarketProtocol, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtobufBitmarketProtocol.class);

    private final PeerConnection localConnection;
    private final PeerServer peerServer;
    private final PeerSession broadcastRemoteSession;
    private final BitmarketProtobuf.BroadcastService.BlockingInterface broadcastRemote;

    private OfferListener offerListener;
    private ExchangeRequestListener exchangeRequestListener;

    public ProtobufBitmarketProtocol(
            PeerConnection broadcastServer) throws BitmarketProtocolException {
        this(broadcastServer, defaultLocalConnection());
    }

    public ProtobufBitmarketProtocol(
            PeerConnection broadcastServer,
            PeerConnection localConnection) throws BitmarketProtocolException {
        this.localConnection = localConnection;
        this.peerServer = this.createPeerServer(localConnection);
        this.broadcastRemoteSession = this.createRemoteSession(broadcastServer);
        this.broadcastRemote = this.createBroadcastRemote(this.broadcastRemoteSession);
        this.offerListener = this.createDefaultOfferListener();
        this.exchangeRequestListener = this.createDefaultOfferAcceptedListener();

        this.peerServer.start();
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
    public void publish(Offer offer) throws BitmarketProtocolException, OfferRejectedException {
        try {
            BitmarketProtobuf.PublishResponse response = this.broadcastRemote.publish(
                    this.broadcastRemoteSession.getController(),
                    ProtobufConversions.toProtobuf(offer));
            switch (response.getResult().getNumber())
            {
                case BitmarketProtobuf.PublishResponse.Result.SERVICE_UNAVAILABLE_VALUE:
                    throw new OfferRejectedException(OfferRejectedException.Reason.BROADCAST_FAILED);
            }
        } catch (ServiceException e) {
            throw new BitmarketProtocolException(
                    "cannot publish offer on remote multicast server", e);
        }
    }

    @Override
    public void requestExchange(
            com.bitwise.bitmarket.common.protocol.ExchangeRequest acceptance,
            PeerConnection recipient) throws BitmarketProtocolException, ExchangeRejectedException {
        try {
            PeerSession session = this.peerServer.peerWith(peerInfo(recipient));
            BitmarketProtobuf.PeerService.BlockingInterface rcpt =
                    BitmarketProtobuf.PeerService.newBlockingStub(session.getChannel());
            BitmarketProtobuf.ExchangeRequestResponse result =  rcpt.requestExchange(
                    session.getController(),
                    ProtobufConversions.toProtobuf(acceptance));
            switch (result.getResult().getNumber())
            {
                case BitmarketProtobuf.ExchangeRequestResponse.Result.INVALID_AMOUNT_VALUE:
                    throw new ExchangeRejectedException(
                            ExchangeRejectedException.Reason.INVALID_AMOUNT);
            }
        } catch (IOException e) {
            throw new BitmarketProtocolException(
                    String.format("IO error while accepting offer %s", acceptance.toString()), e);
        } catch (ServiceException e) {
            throw new BitmarketProtocolException(
                    String.format("service error while accepting offer %s", acceptance.toString()),
                    e);
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

    private BitmarketProtobuf.BroadcastService.BlockingInterface createBroadcastRemote(
            PeerSession session) {
        return BitmarketProtobuf.BroadcastService.newBlockingStub(session.getChannel());
    }

    private BitmarketProtobuf.PeerService.BlockingInterface createPeerRemote(PeerSession session) {
        return BitmarketProtobuf.PeerService.newBlockingStub(session.getChannel());
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
        return new PeerInfo(conn.getHostname(), conn.getPort());
    }

    private static PeerConnection defaultLocalConnection() throws BitmarketProtocolException {
        try {
            return new PeerConnection(InetAddress.getLocalHost().getHostAddress());
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
        public void publish(
                RpcController controller,
                BitmarketProtobuf.Offer offer,
                RpcCallback<BitmarketProtobuf.PublishResponse> done) {
            try {
                offerListener.onOffer(ProtobufConversions.fromProtobuf(offer));
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
                exchangeRequestListener.onExchangeRequest(ProtobufConversions.fromProtobuf(request));
                done.run(successExchangeRequestResponse);
            } catch (ExchangeRejectedException e) {
                done.run(invalidAmountExchangeRequestResponse);
            }
        }
    }
}
