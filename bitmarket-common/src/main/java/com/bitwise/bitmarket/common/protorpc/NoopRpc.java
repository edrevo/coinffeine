package com.bitwise.bitmarket.common.protorpc;

import com.google.protobuf.RpcCallback;

/**
 * No-op RPC callbacks.
 */
public class NoopRpc {

    /**
     * Only one instance is needed. Use the #callback() method instead.
     */
    private NoopRpc() {}

    private static final RpcCallback CALLBACK_INSTANCE = new RpcCallback() {
        @Override
        public void run(Object parameter) {
            // Do nothing
        }
    };

    /**
     * Returns a callback that does nothing.
     */
    @SuppressWarnings("unchecked")
    public static<Message> RpcCallback<Message> callback() {
        return CALLBACK_INSTANCE;
    }
}
