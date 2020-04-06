package xyz.devfortress.functional.pebbles;

/**
 * To be used as a lambda function that can throw random {@link Throwable}
 *
 * @param <X> type of the argument
 * @param <Y> type of return value
 */
@FunctionalInterface
public interface ThrowingFunction<X, Y> {
    Y apply(X argument) throws Throwable;
}
