package xyz.devfortress.functional.pebbles;

/**
 * Visitor to be used with {@link Try#visit(TryVisitor, Object)}.
 *
 * @param <A> type of value contained in the {@code Try} for which this visitor is for
 * @param <C> type if the context
 * @param <R> type of return for all functions in this visitor which is also returned
 *           by {@link Try#visit(TryVisitor, Object)}
 */
public interface TryVisitor<A, C, R> {
    /**
     * Called if {@code Try} is {@code Success(value)}.
     */
    R visitSuccess(A value, C context);

    /**
     * Called if {@code Try} is {@code Failure(error)}.
     */
    R visitFailure(Throwable error, C context);
}
