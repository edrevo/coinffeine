package com.bitwise.bitmarket.common.protocol.protobuf;

import java.util.concurrent.Future;

import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.RpcCallback;

/**
 * An utility class aimed to map a RPC callback into a future.
 *
 * @param <Response>    The response message that will be received via the RPC callback.
 * @param <Result>      The result that will be delivered via the future
 */
public abstract class RpcCallbackFutureMapper<Response, Result> implements RpcCallback<Response> {

    private final SettableFuture<Result> promise;

    public RpcCallbackFutureMapper() {
        this.promise = SettableFuture.create();
    }

    @Override
    public void run(Response response) {
        try {
            this.promise.set(processResponse(response));
        } catch (Throwable e) {
            this.promise.setException(e);
        }
    }

    /**
     * Obtain the future being mapped from the RPC callback.
     */
    public Future<Result> getFuture() {
        return this.promise;
    }

    /**
     * Process the response and map it into a result. This method is aimed to be implemented
     * by concrete mappers in order to process the request, validate it and throw any exception
     * if required, and finally return the corresponding Result object. For a valid response,
     * a Result object is returned that will be set as resulting value of the future managed
     * by the mapper. If a exception is thrown, it will be set as exception object of the
     * future.
     *
     * @param response      The response obtained via the RPC callback
     * @return              The result corresponding to the received response if it is valid
     * @throws Exception    If the response is not valid
     */
    protected abstract Result processResponse(Response response) throws Exception;
}
