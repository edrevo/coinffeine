package com.bitwise.bitmarket.common.protocol.protobuf;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.bitwise.bitmarket.common.ExchangeRejectedException;
import com.bitwise.bitmarket.common.PeerConnection;
import com.bitwise.bitmarket.common.protocol.*;
import com.bitwise.bitmarket.common.protocol.protobuf.BitmarketProtobuf.*;
import com.bitwise.bitmarket.common.protocol.protobuf.BitmarketProtobuf.ExchangeRequest;
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
    private final PeerSession multicastRemoteSession;
    private final PeerService.BlockingInterface multicastRemote;

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
        this.multicastRemoteSession = this.createRemoteSession(broadcastServer);
        this.multicastRemote = this.createRemote(this.multicastRemoteSession);
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
    public void publish(Offer offer) throws BitmarketProtocolException {
        try {
            this.multicastRemote.publish(
                    this.multicastRemoteSession.getController(),
                    ProtobufConversions.toProtobuf(offer));
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
            PeerSession session = this.peerServer.peerWith(peerInfo(localConnection));
            BitmarketProtobuf.PeerService.BlockingInterface rcpt =
                    BitmarketProtobuf.PeerService.newBlockingStub(session.getChannel());
            BitmarketProtobuf.ExchangeRequestResponse result =  rcpt.requestExchange(
                    session.getController(),
                    ProtobufConversions.toProtobuf(acceptance));
            if (result.getResult().equals(BitmarketProtobuf.Result.FAILED)) {
                switch (result.getError()) {
                    case INVALID_AMOUNT:
                        throw new ExchangeRejectedException(
                                ExchangeRejectedException.Reason.INVALID_AMOUNT);
                }
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
        return new PeerServer(peerInfo(localConnection), new PeerServiceImpl());
    }

    private PeerSession createRemoteSession(
            PeerConnection remoteServer) throws BitmarketProtocolException {
        try {
            return this.peerServer.peerWith(peerInfo(localConnection));
        } catch (IOException e) {
            throw new BitmarketProtocolException(
                    String.format(
                            "cannot create session to remote server %s: %s",
                            remoteServer.toString()),
                    e);
        }
    }

    private PeerService.BlockingInterface createRemote(PeerSession session) {
        return PeerService.newBlockingStub(session.getChannel());
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

    private static final VoidResponse okVoidResponse = VoidResponse.newBuilder()
            .setResult(Result.OK)
            .build();

    private static final VoidResponse failedVoidResponse = VoidResponse.newBuilder()
            .setResult(Result.FAILED)
            .build();

    private static final ExchangeRequestResponse okAcceptResponse =
            ExchangeRequestResponse.newBuilder()
                    .setResult(Result.OK)
                    .setError(ExchangeRequestResponse.Error.NO_ERROR)
                    .build();

    private static final ExchangeRequestResponse invalidAmountAcceptResponse =
            ExchangeRequestResponse.newBuilder()
                    .setResult(Result.FAILED)
                    .setError(ExchangeRequestResponse.Error.INVALID_AMOUNT)
                    .build();

    private class PeerServiceImpl extends BitmarketProtobuf.PeerService {

        @Override
        public void publish(
                RpcController controller, PublishOffer offer, RpcCallback<VoidResponse> done) {
            try {
                offerListener.onOffer(ProtobufConversions.fromProtobuf(offer));
                done.run(okVoidResponse);
            } catch (Throwable e) {
                done.run(failedVoidResponse);
            }
        }

        @Override
        public void requestExchange(
                RpcController controller,
                ExchangeRequest request,
                RpcCallback<ExchangeRequestResponse> done) {
            try {
                exchangeRequestListener.onExchangeRequest(ProtobufConversions.fromProtobuf(request));
                done.run(okAcceptResponse);
            } catch (ExchangeRejectedException e) {
                done.run(invalidAmountAcceptResponse);
            }
        }
    }
}
