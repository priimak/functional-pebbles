package xyz.devfortress.functional.pebbles;

/**
 * To be used as a lambda function that can throw arbitrary {@link Throwable}.
 *
 * @param <T> type of return value
 */
@FunctionalInterface
public interface ThrowingSupplier<T> {
    T get() throws Throwable;
}
