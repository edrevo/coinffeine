package com.bitwise.bitmarket.client.paymentprocessor;

import org.apache.axis2.AxisFault;

import com.bitwise.bitmarket.client.paymentprocessor.okpay.OkPayAPIImplementationStub;

public class OkPayFactory {

    public PaymentProcessor build(String token, String endpoint, String userId)
            throws PaymentProcessorException {
        OkPayAPIImplementationStub okpayService = OkPayFactory
                .buildOkpayService(endpoint);
        return new OkPayProcessor(okpayService, token, userId);
    }

    private static final OkPayAPIImplementationStub buildOkpayService(
            String endpoint) {
        OkPayAPIImplementationStub okpayService;
        try {
            okpayService = new OkPayAPIImplementationStub(endpoint);
            return okpayService;
        } catch (AxisFault e) {
            throw new PaymentProcessorException(e.getMessage(), e);
        }
    }
}
