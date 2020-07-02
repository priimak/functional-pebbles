package xyz.devfortress.functional.pebbles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Class that represents the completion of a computation, which may either be a value of type {@code T} in case of
 * success or a {@link Throwable} in case of failure. This is represented by two possible classes,
 * {@code Success<T>(value)} holding value or {@code Failure(Throwable)} holding error.
 * <p></p>
 * <p>
 * Note that since exceptions do not override {@link Object#equals(Object)} and {@link Object#hashCode()} this class
 * does not override these methods if it is a Failure. If, however, this class represents successful result
 * of the computation then both {@code equals(...)} and {@code hashCode()} will work as expected. Calling
 * {@code equals(...)} on {@code Failure} will always returns false unless it is tested against itself.
 * Thus Try should never be used as a key in hash maps or in any other cases where these methods are important.
 * </p>
 * @param <T> type of successfully computed value.
 */
public abstract class Try<T> {
    /**
     * Turns any function lambda function that accepts argument of type X and returns argument of
     * type Y (i.e. {@code X => Y}) and can throw an exception into a function that accepts X and
     * returns {@code Try<Y>} (i.e. {@code X => Try<Y>}). Such operation is known as "lifting".
     *
     * @param f function to be lifted from {@code X => Y} into {@code X => Try<Y>}
     */
    public static <X, Y> Function<X, Try<Y>> lift(ThrowingFunction<X, Y> f) {
        return x -> Try.eval(() -> f.apply(x));
    }

    /**
     * A collector that will partition and collect {@code Stream} of {@code Try}s into a {@link Tuple} containing
     * {@code List} of values contained in {@code Success(value)} and a {@code List} of {@link Throwable} contained
     * in {@code Failure(Throwable)} which are found in the original stream.
     *
     * @param <T> type contained in the {@link Try}s in the steam
     * @return collector for {@code Tuple<List<T>, List<Throwable>>}
     */
    public static <T> Collector<Try<T>, ?, Tuple<List<T>, List<Throwable>>> partition() {
        return new PartitionCollector<>();
    }

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
     * Returns empty {@code Optional} if this is a {@code Failure} or {@code Optional.of(value)} if this
     * is a Success.
     */
    public abstract Optional<T> toOptional();

    /**
     * Converts {@code Optional} into a [@code Success} if it contains value
     * or a {@code Failure(NoSuchElementException)} if it is empty.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <A> Try<A> ofOptional(Optional<A> val) {
        return Try(val::get);
    }

    /**
     * Converts {@code value} into a {@code Success} if it is not null
     * or a {@code Failure(NoSuchElementException)} if it is null.
     */
    public static <A> Try<A> ofNullable(A value) {
        return value == null
            ? Failure(new NoSuchElementException("No value present"))
            : Success(value);
    }

    /**
     * Applies {@code fa(...)} if this is a {@code Failure} or {@code fb} if this is a {@code Success}.
     * If {@code fb(...)} is initially applied and throws an exception, then {@code fa(...)} is applied with
     * this exception.
     *
     * @param fa function applied if this is a {@code Failure} of {@code fb(...)} throws an exception and
     *           results in {@code Failure}
     * @param fb function applied if this is a {@code Success}
     * @return result of applying either {@code fa(...)}, or {@code fb(...)}
     */
    public abstract <A> A fold(Function<Throwable, A> fa, ThrowingFunction<T, A> fb);

    /**
     * Returns this {@code Try} if it's a {@code Success} or the given default argument if this is a {@code Failure}.
     */
    public abstract Try<T> orElse(Try<T> defaultTry);

    /**
     * Returns this {@code Try} if it's a {@code Success} or value returned by the supplier if this
     * is a {@code Failure}.
     */
    public abstract Try<T> orElse(ThrowingSupplier<Try<T>> defaultTrySupplier);

    /**
     * Returns the value from this {@code Success} or the given default argument if this is a {@code Failure}.
     */
    public abstract T getOrElse(T defaultValue);

    /**
     * Returns the value from this {@code Success} or value returned the supplier if this is a {@code Failure}.
     */
    public abstract T getOrElse(Supplier<T> defaultValueSupplier);

    /**
     * Converts this {@code Try} instance into a {@code Failure}. If this {@code Try} is {@code Success} then
     * returns {@code Failure(UnsupportedOperationException)}. Otherwise if this {@code Try} is a {@code Failure}
     * then
     * Completes this Try with an exception wrapped in a Success. The exception is either the exception that
     * the Try failed with (if a Failure) or an UnsupportedOperationException.
     *
     * @return Instance of {@code Try} which is a {@code Failure}.
     */
    public abstract Try<T> failed();

    /**
     * If this instance of {@code Try} is a {@code Success} and a predicate is satisfied (i.e. it returns true), then
     * return {@code Failure(NoSuchElementException)}. If this instance of {@code Try} is a {@code Failure} then this
     * return {@code this} as is, i.e. {@code filter(...)} is no-op in this case.
     */
    public abstract Try<T> filter(ThrowingFunction<T, Boolean> predicate);

    /**
     * Applies the given consumer {@code f} if this is a Success. Noop if this is a Failure.
     */
    public abstract void forEach(Consumer<T> f);

    /**
     * Applies the given function {@code f} if this is a {@code Failure}, otherwise returns this if
     * this is a {@code Success}. This is like a {@code map(...)} function but for the error/exception.
     */
    public abstract Try<T> recoverWith(ThrowingFunction<Throwable, Try<T>> f);

    /**
     * Applies the given function f if this is a Failure, otherwise returns this if this is a Success.
     * This is like a {@code flatMap(...)} function but for the error/exception.
     */
    public abstract Try<T> recover(ThrowingFunction<Throwable, T> f);

    /**
     * Returns true or false indicating whether this instance represents a failure or not.
     */
    public abstract boolean isFailure();

    /**
     * Returns true of false indicating whether this instance represents a successfully computed value or not.
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
        try {
            return new Success<>(f.get());
        } catch (Throwable th) {
            if (th instanceof VirtualMachineError) {
                throw (VirtualMachineError) th;
            } else if (th instanceof ThreadDeath) {
                throw (ThreadDeath) th;
            } else if (th instanceof InterruptedException) {
                throw new RuntimeException(th);
            } else if (th instanceof LinkageError) {
                throw (LinkageError) th;
            } else {
                return new Failure<>(th);
            }
        }
    }

    /**
     * Synonym for {@link #eval(ThrowingSupplier)}. Its intent is to be used with static import to make code that uses
     * {@code Try} monad be more Scala-like. Thus instead of the following
     * <pre>
     *     import xyz.devfortress.functional.pebbles.Try;
     *
     *     Try.eval(() -> { return ...; })
     * </pre>
     * we can write
     * <pre>
     *     import static xyz.devfortress.functional.pebbles.Try.Try;
     *
     *     Try(() -> { return ...; })
     * </pre>
     *
     */
    @SuppressWarnings("MethodNameSameAsClassName")
    public static <T> Try<T> Try(ThrowingSupplier<T> f) {
        try {
            return new Success<>(f.get());
        } catch (Throwable th) {
            if (th instanceof VirtualMachineError) {
                throw (VirtualMachineError) th;
            } else if (th instanceof ThreadDeath) {
                throw (ThreadDeath) th;
            } else if (th instanceof InterruptedException) {
                throw new RuntimeException(th);
            } else if (th instanceof LinkageError) {
                throw (LinkageError) th;
            } else {
                return new Failure<>(th);
            }
        }
    }

    /**
     * Returns the value in case of success, or throws the {@link Throwable} in case of failure.
     */
    public abstract T get() throws Throwable;

    /**
     * Maps value contained in this Try success instance by applying function {@code f} to this value and wrapping
     * result into new instance of {@code Try<A>} object. If this Try instance represents failure then this function
     * is no-op and function {@code f} is never called. Every non-fatal exception thrown when {@code f(...)} is called
     * will result in {@link Try#failure(Throwable)}. Following exception are considered fatal
     * <ul>
     *     <li>{@link VirtualMachineError}</li>
     *     <li>{@link ThreadDeath}</li>
     *     <li>{@link InterruptedException}</li>
     *     <li>{@link LinkageError}</li>
     * </ul>
     * if thrown in {@code f(...)} they will be re-thrown out of the {@code map(...)} call.
     * Exception {@code InterruptedException} will be wrapped into {@link RuntimeException}.
     *
     * @param f function to be applied to the current value contained in the Try success instance
     * @param <A> type of value returned by function {@code f}
     * @return result computing function {@code f} wrapped into {@link Try} instance or original failure
     */
    public abstract <A> Try<A> map(ThrowingFunction<T, A> f);

    /**
     * Maps value contained in this Try success instance by applying function {@code f} to this value and returning it.
     * If this Try instance represents failure then this function is a no-op, failure is returned and function {@code f}
     * is never called.
     *
     * @param f function to be applied to the current value contained in the Try success instance
     * @param <A> type of value enclosed in the returned {@code Try} instance when function {@code f} is called
     * @return result of calling function  {@code f} or original failure
     */
    public abstract <A> Try<A> flatMap(ThrowingFunction<T, Try<A>> f);

    /**
     * Transforms this instance of {@code Try} into value of type {@code A}.
     *
     * @param onSuccess function to be called if this instance of {@code Try} represents success
     * @param onFailure function to be called if this instance of {@code Try} represents failure
     * @param <A> type of result returned by {@code onSuccess} and {@code onFailure} functions
     * @return result of calling either {@code onSuccess} and {@code onFailure} functions
     */
    public abstract <A> A transform(Function<T, A> onSuccess, Function<Throwable, A> onFailure);

    /**
     * Accepts this instance of {@code Try} into either {@code onSuccess} consumer of {@codo Success} containing
     * value or into {@code onFailure} containing error.
     */
    public abstract void accept(Consumer<T> onSuccess, Consumer<Throwable> onFailure);

    /**
     * Visitor pattern call analogous to {@link #transform(Function, Function)}
     */
    public abstract <C, R> R visit(TryVisitor<T, C, R> visitor, C context);

    /**
     * Synonym for {@link #map(ThrowingFunction)}.
     */
    public <A> Try<A> andThen(ThrowingFunction<T, A> f) {
        return map(f);
    }

    /**
     * Convenience method to be used with static import that mimics behavior of case classes in Scala. It can be used
     * to create instances of successful computation instead of {@link Try#success(Object)} like so
     * <pre>
     *     import static xyz.devfortress.functional.pebbles.Try.Success;
     *
     *     result.flatMap(value -> Success(value + " World"));
     * </pre>
     */
    public static <A> Try<A> Success(A value) {
        return new Success<>(value);
    }

    /**
     * Convenience method to be used with static import that mimics behavior of case classes in Scala. It can be used
     * to create instances of successful computation instead of {@link Try#failure(Throwable)} like so
     * <pre>
     *     import static xyz.devfortress.functional.pebbles.Try.Failure;
     *
     *     result.flatMap(value -> Failure(new Exception()));
     * </pre>
     */
    public static <A> Try<A> Failure(Throwable error) {
        return new Failure<>(error);
    }

    /**
     * Result of successful computation.
     *
     * @param <T> type of successfully computed value
     */
    private static final class Success<T> extends Try<T> {
        private final T value;

        Success(T value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (other instanceof Success) {
                return Objects.equals(value, ((Success<?>) other).value);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return "Success(" + value + ")";
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.of(value);
        }

        @Override
        public <A> A fold(Function<Throwable, A> fa, ThrowingFunction<T, A> fb) {
            try {
                return fb.apply(value);
            } catch (Throwable th) {
                if (th instanceof VirtualMachineError) {
                    throw (VirtualMachineError) th;
                } else if (th instanceof ThreadDeath) {
                    throw (ThreadDeath) th;
                } else if (th instanceof InterruptedException) {
                    throw new RuntimeException(th);
                } else if (th instanceof LinkageError) {
                    throw (LinkageError) th;
                } else {
                    return fa.apply(th);
                }
            }
        }

        @Override
        public Try<T> orElse(Try<T> defaultTry) {
            return this;
        }

        @Override
        public Try<T> orElse(ThrowingSupplier<Try<T>> defaultTrySupplier) {
            return this;
        }

        @Override
        public T getOrElse(T defaultValue) {
            return value;
        }

        @Override
        public T getOrElse(Supplier<T> defaultValueSupplier) {
            return value;
        }

        @Override
        public Try<T> failed() {
            return new Failure<>(new UnsupportedOperationException("Success.failed"));
        }

        @Override
        public Try<T> filter(ThrowingFunction<T, Boolean> predicate) {
            try {
                return predicate.apply(value)
                    ? this
                    : new Failure<>(new NoSuchElementException("Predicate does not hold for " + value));
            } catch (Throwable th) {
                if (th instanceof VirtualMachineError) {
                    throw (VirtualMachineError) th;
                } else if (th instanceof ThreadDeath) {
                    throw (ThreadDeath) th;
                } else if (th instanceof InterruptedException) {
                    throw new RuntimeException(th);
                } else if (th instanceof LinkageError) {
                    throw (LinkageError) th;
                } else {
                    return new Failure<>(th);
                }
            }
        }

        @Override
        public void forEach(Consumer<T> f) {
            f.accept(value);
        }

        @Override
        public Try<T> recoverWith(ThrowingFunction<Throwable, Try<T>> f) {
            return this;
        }

        @Override
        public Try<T> recover(ThrowingFunction<Throwable, T> f) {
            return this;
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
                return new Success<>(f.apply(value));
            } catch (Throwable th) {
                if (th instanceof VirtualMachineError) {
                    throw (VirtualMachineError) th;
                } else if (th instanceof ThreadDeath) {
                    throw (ThreadDeath) th;
                } else if (th instanceof InterruptedException) {
                    throw new RuntimeException(th);
                } else if (th instanceof LinkageError) {
                    throw (LinkageError) th;
                } else {
                    return new Failure<>(th);
                }
            }
        }

        @Override
        public <A> Try<A> flatMap(ThrowingFunction<T, Try<A>> f) {
            try {
                return f.apply(value);
            } catch (Throwable th) {
                if (th instanceof VirtualMachineError) {
                    throw (VirtualMachineError) th;
                } else if (th instanceof ThreadDeath) {
                    throw (ThreadDeath) th;
                } else if (th instanceof InterruptedException) {
                    throw new RuntimeException(th);
                } else if (th instanceof LinkageError) {
                    throw (LinkageError) th;
                } else {
                    return new Failure<>(th);
                }
            }
        }

        @Override
        public <A> A transform(Function<T, A> onSuccess, Function<Throwable, A> onFailure) {
            return onSuccess.apply(value);
        }

        @Override
        public void accept(Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
            onSuccess.accept(value);
        }

        @Override
        public <C, R> R visit(TryVisitor<T, C, R> visitor, C context) {
            return visitor.visitSuccess(value, context);
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
        public boolean equals(Object other) {
            return this == other;
        }

        @Override
        public String toString() {
            return "Failure(" + error.getClass().getSimpleName() + ")";
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
        }

        @Override
        public <A> A fold(Function<Throwable, A> fa, ThrowingFunction<T, A> fb) {
            return fa.apply(error);
        }

        @Override
        public Try<T> orElse(Try<T> defaultTry) {
            return defaultTry;
        }

        @Override
        public Try<T> orElse(ThrowingSupplier<Try<T>> defaultTrySupplier) {
            try {
                return defaultTrySupplier.get();
            } catch (Throwable th) {
                if (th instanceof VirtualMachineError) {
                    throw (VirtualMachineError) th;
                } else if (th instanceof ThreadDeath) {
                    throw (ThreadDeath) th;
                } else if (th instanceof InterruptedException) {
                    throw new RuntimeException(th);
                } else if (th instanceof LinkageError) {
                    throw (LinkageError) th;
                } else {
                    return new Failure<>(th);
                }
            }
        }

        @Override
        public T getOrElse(T defaultValue) {
            return defaultValue;
        }

        @Override
        public T getOrElse(Supplier<T> defaultValueSupplier) {
            return defaultValueSupplier.get();
        }

        @Override
        public Try<T> failed() {
            return this;
        }

        @Override
        public Try<T> filter(ThrowingFunction<T, Boolean> predicate) {
            return this;
        }

        @Override
        public void forEach(Consumer<T> f) {
            // noop
        }

        @Override
        public Try<T> recoverWith(ThrowingFunction<Throwable, Try<T>> f) {
            try {
                return f.apply(error);
            } catch (Throwable th) {
                if (th instanceof VirtualMachineError) {
                    throw (VirtualMachineError) th;
                } else if (th instanceof ThreadDeath) {
                    throw (ThreadDeath) th;
                } else if (th instanceof InterruptedException) {
                    throw new RuntimeException(th);
                } else if (th instanceof LinkageError) {
                    throw (LinkageError) th;
                } else {
                    return new Failure<>(th);
                }
            }
        }

        @Override
        public Try<T> recover(ThrowingFunction<Throwable, T> f) {
            try {
                return new Success<>(f.apply(error));
            } catch (Throwable th) {
                if (th instanceof VirtualMachineError) {
                    throw (VirtualMachineError) th;
                } else if (th instanceof ThreadDeath) {
                    throw (ThreadDeath) th;
                } else if (th instanceof InterruptedException) {
                    throw new RuntimeException(th);
                } else if (th instanceof LinkageError) {
                    throw (LinkageError) th;
                } else {
                    return new Failure<>(th);
                }
            }
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
        public <A> Try<A> flatMap(ThrowingFunction<T, Try<A>> f) {
            return (Try<A>) this;
        }

        @Override
        public <A> A transform(Function<T, A> onSuccess, Function<Throwable, A> onFailure) {
            return onFailure.apply(error);
        }

        @Override
        public void accept(Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
            onFailure.accept(error);
        }

        @Override
        public <C, R> R visit(TryVisitor<T, C, R> visitor, C context) {
            return visitor.visitFailure(error, context);
        }
    }

    private static class PartitionCollector<T> implements
            Collector<Try<T>, Tuple<ArrayList<T>, ArrayList<Throwable>>, Tuple<List<T>, List<Throwable>>> {
        private static final Set<Collector.Characteristics> CH_ID
            = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));

        @Override
        public Supplier<Tuple<ArrayList<T>, ArrayList<Throwable>>> supplier() {
            return () -> new Tuple<>(new ArrayList<T>(), new ArrayList<Throwable>());
        }

        @Override
        public BiConsumer<Tuple<ArrayList<T>, ArrayList<Throwable>>, Try<T>> accumulator() {
            return (acc, tVal) -> tVal.accept(acc._1::add, acc._2::add);
        }

        @Override
        public BinaryOperator<Tuple<ArrayList<T>, ArrayList<Throwable>>> combiner() {
            return (left, right) -> {
                left._1.addAll(right._1);
                left._2.addAll(right._2);
                return left;
            };
        }

        @Override
        public Function<Tuple<ArrayList<T>, ArrayList<Throwable>>, Tuple<List<T>, List<Throwable>>> finisher() {
            return acc -> new Tuple<>(Collections.unmodifiableList(acc._1), Collections.unmodifiableList(acc._2));
        }

        @Override
        public Set<Characteristics> characteristics() {
            return CH_ID;
        }
    }

}
