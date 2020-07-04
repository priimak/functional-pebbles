package xyz.devfortress.functional.pebbles;

import java.util.function.Function;

/**
 * This is a synonym for a function {@code X => Try<Y>} with additional functions {@link #map(ThrowingFunction)} and
 * {@link #flatMap(ThrowingFunction)} used in for composing other functions.
 *
 * @param <X> the type of the input to the function
 * @param <Y> the type in {@link Try} returned by the function
 */
@FunctionalInterface
public interface TryFunction<X, Y> extends Function<X, Try<Y>> {
    /**
     * Creates a new function {@code X => Try<Z>} which is when applied first evaluates {@code X => Try<Y>} and then,
     * if {@code Try<Y>} is {@code Success(Y)}, evaluates {@code Y => Try<Z>}.
     *
     * @param after function to be evaluated after this function if it returns {@code Success(Y)}. Its result is
     *              wrapped into {@code Try<Z>} and returned. If it throws exception, then it
     *              returns {@code Failure(Throwable)}
     * @param <Z> return value of {@code after.apply(Y)}
     * @return new combined function mapping from X to {@code Try<Z>} (i.e. {@code X => Try<Z>})
     */
    default <Z> TryFunction<X, Z> map(ThrowingFunction<Y, Z> after) {
        return x -> apply(x).map(after);
    }

    /**
     * Creates a new function {@code X => Try<Z>} which is when applied first evaluates {@code X => Try<Y>} and then,
     * if {@code Try<Y>} is {@code Success(Y)}, evaluates {@code Y => Try<Z>}.
     *
     * @param after function to be evaluated after this function if it returns {@code Success(Y)}. Since its result is
     *              {@code Try<Z>} it is returned as it is. If it throws exception, then it
     *              returns {@code Failure(Throwable)}
     * @param <Z> type of {@link Try} returned when {@code after.apply(Y)} is called
     * @return new combined function mapping from X to {@code Try<Z>} (i.e. {@code X => Try<Z>})
     */
    default <Z> TryFunction<X, Z> flatMap(ThrowingFunction<Y, Try<Z>> after) {
        return x -> apply(x).flatMap(after);
    }
}
