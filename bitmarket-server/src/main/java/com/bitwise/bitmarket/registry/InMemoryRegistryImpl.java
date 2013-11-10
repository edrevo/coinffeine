package com.bitwise.bitmarket.registry;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

import com.bitwise.bitmarket.common.protocol.Offer.*;

public class InMemoryRegistryImpl implements RegistryService.Interface {

    private static final VoidResponse OK = VoidResponse.newBuilder().setResult(Result.OK).build();

    @Override
    public void registerClient(
            RpcController controller, RegistrationRequest request, RpcCallback<VoidResponse> done) {
        System.out.println(request.getConnection() + " registered!");
        done.run(OK);
    }

    @Override
    public void publish(
            RpcController controller, PublishOffer request, RpcCallback<VoidResponse> done) {
        System.out.println(request.getConnection() + " published an offer: " + request);
        done.run(OK);
    }
}
