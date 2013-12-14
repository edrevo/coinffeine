package com.bitwise.bitmarket.common.util;

/**
 * Utility functions to handle non-trivial actions on exception objects.
 */
public class ExceptionUtils {

    /**
     * Create a new exception utils object ready to operate on given exception.
     */
    public static ExceptionUtils forException(Throwable e) {
        return new ExceptionUtils(e);
    }

    private final Throwable e;

    private ExceptionUtils(Throwable e) {
        this.e = e;
    }

    /**
     * Throw the cause of the exception if it matches certain type. If there is no cause, nothing
     * is done.
     *
     * @param expectedCause The class object of the expected cause.
     * @param <T>           The type of the expected cause object.
     * @return              The exception utils object if cause doesn't match and it is not thrown.
     * @throws T            The cause if it matches the expected type.
     */
    public <T extends Throwable> ExceptionUtils throwCauseIfMatch(
            Class<T> expectedCause) throws T {
        Throwable cause = this.e.getCause();
        if (expectedCause.isInstance(cause))
            throw (T) cause;
        return this;
    }

    /**
     * Throw the cause of the exception if it matches certain type. If there is no cause, nothing
     * is done.
     *
     * @param expectedCause The class object of the expected cause.
     * @param <T>           The type of the expected cause object.
     * @return              The exception utils object if cause doesn't match and it is not thrown.
     * @throws T            The cause if it matches the expected type.
     */
    public <T extends Throwable> ExceptionUtils otherwiseThrowCauseIfMatch(
            Class<T> expectedCause) throws T {
        return throwCauseIfMatch(expectedCause);
    }

    /**
     * Wrap the cause of the exception on given exception type and throw it. If cause is not set,
     * the wrapped exception object will be thrown with no cause.
     *
     * @param wrapperClass  The class of the object to wrap the cause of the exception.
     * @param <T>           The type of the object to wrap the cause of the exception.
     * @throws T            The exception object that wraps the cause of the exception.
     */
    public <T extends Throwable> void wrapCauseAndThrow(Class<T> wrapperClass) throws T {
        T wrapper = null;
        try {
            wrapper = wrapperClass.getConstructor(Throwable.class).newInstance(this.e.getCause());
        } catch (Throwable e) {
            throw new RuntimeException(
                    String.format(
                            "cannot invoke appropriate constructor for exception class %s",
                            wrapperClass.getCanonicalName()));
        }
        throw wrapper;
    }

    /**
     * Wrap the cause of the exception on given exception type and throw it. If cause is not set,
     * the wrapped exception object will be thrown with no cause.
     *
     * @param wrapperClass  The class of the object to wrap the cause of the exception.
     * @param <T>           The type of the object to wrap the cause of the exception.
     * @throws T            The exception object that wraps the cause of the exception.
     */
    public <T extends Throwable> void otherwiseWrapCauseAndThrow(Class<T> wrapperClass) throws T {
        wrapCauseAndThrow(wrapperClass);
    }

}
