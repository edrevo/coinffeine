package com.bitwise.bitmarket.common.protocol.protobuf;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.bitwise.bitmarket.common.PeerConnection;
import com.bitwise.bitmarket.common.protocol.BitmarketProtocol;
import com.bitwise.bitmarket.common.protocol.BitmarketProtocolException;
import com.bitwise.bitmarket.common.protocol.Offer;
import com.bitwise.bitmarket.common.protocol.OfferListener;
import com.bitwise.bitmarket.common.protocol.protobuf.OfferProtocol.PeerService;
import com.bitwise.bitmarket.common.protocol.protobuf.OfferProtocol.PublishOffer;
import com.bitwise.bitmarket.common.protocol.protobuf.OfferProtocol.VoidResponse;
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
    public void publish(Offer offer) throws BitmarketProtocolException {
        try {
            this.multicastRemote.publish(
                    this.multicastRemoteSession.getController(),
                    OfferConversions.toProtobuf(offer));
        } catch (ServiceException e) {
            throw new BitmarketProtocolException(
                    "cannot publish offer on remote multicast server", e);
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

    private class PeerServiceImpl extends OfferProtocol.PeerService {

        @Override
        public void publish(
                RpcController controller, PublishOffer offer, RpcCallback<VoidResponse> done) {
            offerListener.onOffer(OfferConversions.fromProtobuf(offer));
        }
    }
}
