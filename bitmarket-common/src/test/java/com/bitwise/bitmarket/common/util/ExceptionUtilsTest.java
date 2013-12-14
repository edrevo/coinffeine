package com.bitwise.bitmarket.common.util;

import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;

public class ExceptionUtilsTest {

    @Test
    public void mustThrowIfCauseMatch() throws Exception {
        IllegalArgumentException cause = new IllegalArgumentException("foobar");
        try {
            ExceptionUtils
                    .forException(makeExceptionWithCause(cause))
                    .throwCauseIfMatch(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertEquals(cause, e);
        }
    }

    @Test
    public void mustNotThrowIfCauseDoesNotMatch() throws Exception {
        ExceptionUtils
                .forException(makeExceptionWithCause(new IllegalArgumentException("")))
                .throwCauseIfMatch(NoSuchElementException.class);
    }

    @Test
    public void mustNotThrowIfCauseDoesNotExists() throws Exception {
        ExceptionUtils
                .forException(makeExceptionWithCause(null))
                .throwCauseIfMatch(NoSuchElementException.class);
    }

    @Test
    public void mustWrapCauseAndThrow() throws Exception {
        IllegalArgumentException cause = new IllegalArgumentException("foobar");
        try {
            ExceptionUtils
                    .forException(makeExceptionWithCause(cause))
                    .wrapCauseAndThrow(RuntimeException.class);
        } catch (RuntimeException e) {
            assertEquals(cause, e.getCause());
        }
    }

    @Test
    public void mustWrapNonExistingCauseAndThrow() throws Exception {
        try {
            ExceptionUtils
                    .forException(makeExceptionWithCause(null))
                    .wrapCauseAndThrow(RuntimeException.class);
        } catch (RuntimeException e) {
            assertNull(e.getCause());
        }
    }

    @Test(expected = RuntimeException.class)
    public void mustThrowOnWrapCauseWithInappropriateWrapperClass() throws Exception {
        ExceptionUtils
                .forException(makeExceptionWithCause(new IllegalArgumentException("foobar")))
                .wrapCauseAndThrow(NoSuchElementException.class);
    }

    private Throwable makeExceptionWithCause(Throwable cause) {
        return new ExecutionException(cause);
    }
}
