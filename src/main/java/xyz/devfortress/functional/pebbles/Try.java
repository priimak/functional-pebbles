package xyz.devfortress.functional.pebbles;

import java.util.function.Function;

/**
 * Class that represents the completion of a computation, which may either be a value of type {@code T} in case of
 * success or a {@link Throwable} in case of failure.
 * <p>
 * Note that since exceptions do not override {@link Object#equals(Object)} and {@link Object#hashCode()} this class
 * does not override these methods either. Thus it should not be used as a key in hash maps or in any other functions
 * where these methods are important.
 *
 * @param <T> type of successfully computed value.
 */
public abstract class Try<T> {
    /**
     * Returns a new instance representing successful computation with the given value.
     *
     * @param value the value
     * @param <T> type of successfully computed value
     */
    public static <T> Try<T> success(T value) {
        return new Success<>(value);
    }

    /**
     * Returns a new instance representing failed computation with the given {@link Throwable}.
     *
     * @param throwable the {@link Throwable} instance
     * @param <T> type of successfully computed value
     */
    public static <T> Try<T> failure(Throwable throwable) {
        return new Failure<>(throwable);
    }

    private Try() { }

    /**
     * Returns whether this instance represents a failure.
     */
    public abstract boolean isFailure();

    /**
     * Returns whether this instance represents a successfully computed value.
     */
    public abstract boolean isSuccess();

    /**
     * Evaluate lambda function (supplier) returning either successful of failed computation result. This method allows
     * one to evaluate computation that can throw and exception, capture possible successful or failed (exception
     * thrown) result wrapped in the instance of {@link Try}.
     * <p>
     * Note that instances of {@link RuntimeException} and {@link Error} exceptions will not be captured and will be
     * will be allowed to propagate.
     *
     * @param f supplier to evaluate
     * @param <T> type of the successful result
     * @return instance of {@link Try} that encapsulates result of the computation
     */
    public static <T> Try<T> eval(ThrowingSupplier<T> f) {
        return Success.VOID_INSTANCE.map($ -> f.get());
    }

    /**
     * Returns the value in case of success, or throws the {@link Throwable} in case of failure.
     */
    public abstract T get() throws Throwable;

    public abstract <A> Try<A> map(ThrowingFunction<T, A> f);

    public abstract <A> Try<A> flatMap(Function<T, Try<A>> f);

    public abstract <A> A visit(Function<T, A> onSuccess, Function<Throwable, A> onFailure);

    /**
     * Synonym for {@link #map(ThrowingFunction)}.
     */
    public <A> Try<A> andThen(ThrowingFunction<T, A> f) {
        return map(f);
    }

    /**
     * Result of successful computation.
     *
     * @param <T> type of successfully computed value
     */
    private static final class Success<T> extends Try<T> {
        public static final Success<Void> VOID_INSTANCE = new Success<>(null);

        private final T value;

        Success(T value) {
            this.value = value;
        }

        @Override
        public boolean isFailure() {
            return false;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public <A> Try<A> map(ThrowingFunction<T, A> f) {
            try {
                return Try.success(f.apply(value));
            } catch (Throwable th) {
                if (th instanceof RuntimeException) {
                    throw (RuntimeException) th;
                } else if (th instanceof Error) {
                    throw (Error) th;
                } else {
                    return Try.failure(th);
                }
            }
        }

        @Override
        public <A> Try<A> flatMap(Function<T, Try<A>> f) {
            return f.apply(value);
        }

        @Override
        public <A> A visit(Function<T, A> onSuccess, Function<Throwable, A> onFailure) {
            return onSuccess.apply(value);
        }
    }

    /**
     * Result of failed computation.
     *
     * @param <T> type of successfully computed value
     */
    private static final class Failure<T> extends Try<T> {
        private final Throwable error;

        Failure(Throwable throwable) {
            this.error = throwable;
        }

        @Override
        public boolean isFailure() {
            return true;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public T get() throws Throwable {
            throw error;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <A> Try<A> map(ThrowingFunction<T, A> f) {
            return (Try<A>) this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <A> Try<A> flatMap(Function<T, Try<A>> f) {
            return (Try<A>) this;
        }

        @Override
        public <A> A visit(Function<T, A> onSuccess, Function<Throwable, A> onFailure) {
            return onFailure.apply(error);
        }
    }
}
