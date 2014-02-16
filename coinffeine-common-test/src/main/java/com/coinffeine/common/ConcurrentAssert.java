package com.coinffeine.common;

/**
 * Assertion methods useful for concurrent tests.
 */
public final class ConcurrentAssert {

    public static final int DEFAULT_TIMEOUT = 1000;
    public static final int DEFAULT_RETRY_WAIT = 100;

    private ConcurrentAssert() {}

    /**
     * Repeat an assertion until it doesn't fail or a timeout is reached.
     *
     * @param assertion        Assertion to check
     * @param timeoutInMillis  Timeout
     * @param retryWaitMillis  Time to wait between attempts
     */
    public static void assertEventually(Runnable assertion, int timeoutInMillis, int retryWaitMillis) {
        long timeLimit = System.currentTimeMillis() + timeoutInMillis;
        Throwable lastException;
        do {
            try {
                assertion.run();
                return;
            } catch (RuntimeException e) {
                lastException = e;
            } catch (AssertionError e) {
                lastException = e;
            }
            if (System.currentTimeMillis() >= timeLimit) {
                break;
            }
            try {
                Thread.sleep(retryWaitMillis);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } while (System.currentTimeMillis() < timeLimit);
        throw new RuntimeException(lastException);
    }

    /**
     * Repeat an assertion until it doesn't fail or a timeout is reached. Default values for
     * the timeout and retry wait are used.
     *
     * @param assertion  Assertion to check
     * @see #assertEventually(Runnable, int, int)
     */
    public static void assertEventually(Runnable assertion) {
        assertEventually(assertion, DEFAULT_TIMEOUT, DEFAULT_RETRY_WAIT);
    }
}
