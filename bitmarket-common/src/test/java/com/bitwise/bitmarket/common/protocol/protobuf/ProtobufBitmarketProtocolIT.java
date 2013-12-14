package com.bitwise.bitmarket.common.protocol.protobuf;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;
import com.googlecode.protobuf.pro.duplex.PeerInfo;
import com.googlecode.protobuf.pro.duplex.RpcClientChannel;
import com.googlecode.protobuf.pro.duplex.server.RpcClientRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bitwise.bitmarket.common.PeerConnection;
import com.bitwise.bitmarket.common.currency.BtcAmount;
import com.bitwise.bitmarket.common.currency.FiatAmount;
import com.bitwise.bitmarket.common.protocol.*;
import com.bitwise.bitmarket.common.protocol.ExchangeRejectedException.Reason;
import com.bitwise.bitmarket.common.protorpc.NoopRpc;
import com.bitwise.bitmarket.common.protorpc.PeerServer;
import com.bitwise.bitmarket.common.util.ExceptionUtils;

import static org.junit.Assert.assertEquals;

public class ProtobufBitmarketProtocolIT {

    private BroadcastServer server;
    private List<Peer> peers;
    private Offer sampleOffer;
    private ExchangeRequest sampleExchangeRequest;

    @Before
    public void setUp() throws BitmarketProtocolException {
        this.server = new BroadcastServer();
        this.peers = new ArrayList<>(2);
        this.peers.add(0, new Peer(1234));
        this.peers.add(0, new Peer(1235));

        this.sampleOffer = new Offer(
                new OfferId(7000),
                0,
                this.peers.get(0).getId(),
                this.peers.get(0).getConnection(),
                new BtcAmount(new BigDecimal(2)),
                new FiatAmount(new BigDecimal(2), Currency.getInstance("EUR")));
        this.sampleExchangeRequest = new ExchangeRequest(
                new OfferId(7000),
                this.peers.get(0).getId(),
                this.peers.get(0).getConnection(),
                new BtcAmount(new BigDecimal(2)));
    }

    @After
    public void tearDown() throws Exception {
        for (Peer p : this.peers) {
            p.close();
        }
        this.server.close();
    }

    @Test
    public void mustRequestAndReceiveOfferBroadcast() throws Exception {
        this.peers.get(0).broadcastOffer(this.sampleOffer);

        assertEquals(7000, this.peers.get(1).getOffers().get(0).getId().getBytes());
    }

    @Test(expected = OfferRejectedException.class)
    public void mustThrowOnRejectedBroadcast() throws Exception {
        this.server.prepareToRejectOffers();
        this.peers.get(0).broadcastOffer(this.sampleOffer);
    }

    @Test
    public void mustRequestExchange() throws Exception {
        this.peers.get(0).requestExchange(
                this.sampleExchangeRequest,
                this.peers.get(1).getConnection());
        assertEquals(7000, this.peers.get(1).getExchangeRequests().get(0).getId().getBytes());
    }

    @Test(expected = ExchangeRejectedException.class)
    public void mustThrowOnRejectedExchangeRequest() throws Exception {
        this.peers.get(1).prepareToRejectExchangeRequests();
        this.peers.get(0).requestExchange(
                this.sampleExchangeRequest,
                this.peers.get(1).getConnection());
    }

    private static final int BROADCAST_SERVER_PORT = 9876;

    private static class Peer implements AutoCloseable {

        private final PeerId id;
        private final PeerConnection connection;
        private final BitmarketProtocol bitmarket;
        private final List<Offer> offers;
        private final List<ExchangeRequest> exchangeRequests;

        private boolean rejectExchangeRequest;

        public Peer(int port) throws BitmarketProtocolException {
            this.id = new PeerId(String.format("peer-tcp-%d", port));
            this.connection = new PeerConnection("localhost", port);
            this.offers = new LinkedList<>();
            this.exchangeRequests = new LinkedList<>();
            this.rejectExchangeRequest = false;

            PeerConnection broadcastServerConn = new PeerConnection(
                    "localhost", BROADCAST_SERVER_PORT);
            this.bitmarket = new ProtobufBitmarketProtocol(broadcastServerConn, this.connection);

            this.bitmarket.setOfferListener(new OfferListener() {
                @Override
                public void onOffer(Offer offer) {
                    offers.add(offer);
                }
            });
            this.bitmarket.setExchangeRequestListener(new ExchangeRequestListener() {
                @Override
                public void onExchangeRequest(ExchangeRequest request)
                        throws ExchangeRejectedException {
                    if (rejectExchangeRequest) {
                        throw new ExchangeRejectedException(Reason.INVALID_AMOUNT);
                    }
                    exchangeRequests.add(request);
                }
            });
        }

        public PeerId getId() {
            return this.id;
        }

        public PeerConnection getConnection() {
            return this.connection;
        }

        public List<Offer> getOffers() {
            return this.offers;
        }

        public List<ExchangeRequest> getExchangeRequests() {
            return this.exchangeRequests;
        }

        public void prepareToRejectExchangeRequests() {
            this.rejectExchangeRequest = true;
        }

        @Override
        public void close() throws Exception {
            this.bitmarket.close();
        }

        public void broadcastOffer(Offer offer)
                throws BitmarketProtocolException, OfferRejectedException {
            try {
                this.bitmarket.publish(offer).get();
            } catch (InterruptedException e) {
                throw new BitmarketProtocolException(e);
            } catch (ExecutionException e) {
                ExceptionUtils
                        .forException(e)
                        .throwCauseIfMatch(OfferRejectedException.class)
                        .otherwiseWrapCauseAndThrow(BitmarketProtocolException.class);
            }
        }

        public void requestExchange(ExchangeRequest req, PeerConnection recipient)
                throws BitmarketProtocolException, ExchangeRejectedException {
            try {
                this.bitmarket.requestExchange(req, recipient).get();
            } catch (InterruptedException e) {
                throw new BitmarketProtocolException(e);
            } catch (ExecutionException e) {
                ExceptionUtils
                        .forException(e)
                        .throwCauseIfMatch(ExchangeRejectedException.class)
                        .otherwiseWrapCauseAndThrow(BitmarketProtocolException.class);
            }
        }
    }

    private static class BroadcastServer implements AutoCloseable {

        private final PeerServer server;
        private final RpcClientRegistry clientRegistry;
        private boolean rejectOffers;

        public BroadcastServer() {
            this.rejectOffers = false;

            PeerInfo pinfo = new PeerInfo("localhost", BROADCAST_SERVER_PORT);
            Service srv = BitmarketProtobuf.BroadcastService.newReflectiveService(new Handler());
            this.server = new PeerServer(pinfo, srv);
            this.clientRegistry = this.server.getClientRegistry();

            this.server.start();
        }

        public void prepareToRejectOffers() {
            this.rejectOffers = true;
        }

        @Override
        public void close() throws Exception {
            this.server.shutdown();
        }

        private class Handler implements BitmarketProtobuf.BroadcastService.Interface {

            @Override
            public void publish(
                    RpcController controller,
                    BitmarketProtobuf.Offer request,
                    RpcCallback<BitmarketProtobuf.PublishResponse> done) {
                if (rejectOffers) {
                    BitmarketProtobuf.PublishResponse.Result result =
                            BitmarketProtobuf.PublishResponse.Result.SERVICE_UNAVAILABLE;
                    done.run(BitmarketProtobuf.PublishResponse.newBuilder()
                            .setResult(result)
                            .build());
                } else {
                    for (RpcClientChannel channel : clientRegistry.getAllClients()) {
                        BitmarketProtobuf.PeerService.Stub peer =
                                BitmarketProtobuf.PeerService.newStub(channel);
                        peer.publish(
                                channel.newRpcController(),
                                request,
                                NoopRpc.<BitmarketProtobuf.PublishResponse>callback());
                    }
                    done.run(BitmarketProtobuf.PublishResponse.newBuilder()
                            .setResult(BitmarketProtobuf.PublishResponse.Result.SUCCESS)
                            .build());
                }
            }
        }
    }
}
