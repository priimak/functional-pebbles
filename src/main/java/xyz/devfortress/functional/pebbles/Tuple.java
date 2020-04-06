package xyz.devfortress.functional.pebbles;

import java.util.Objects;

/**
 * Class that encapsulated pair of values of type L and R. This object will properly handle both {@code equals(Object)}
 * and {@code hashCode()} method assuming that both values correctly handle these methods as well.
 * <p>
 * Field values are accessed directly by accessing field variables {@code _1} and {@code _2} similar to have these
 * values are accessed in Scala.
 *
 * @param <L> type of left (first) value
 * @param <R> type of right (second) value
 */
public final class Tuple<L, R> {
    /**
     * First (left) field in the tuple.
     */
    public final L _1;

    /**
     * Second (right) field in the tuple.
     */
    public final R _2;

    public Tuple(L _1, R _2) {
        this._1 = _1;
        this._2 = _2;
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        } else {
            Tuple<?, ?> another = (Tuple<?, ?>) otherObject;
            return Objects.equals(_1, another._1)
                && Objects.equals(_2, another._2);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(_1, _2);
    }

    /**
     * Helper method to be used instead of explicitly calling constructor {@code new Tuple(...)} to create
     * instance of {@link Tuple}.
     */
    public static <A, B> Tuple<A, B> valueOf(A leftValue, B rightValue) {
        return new Tuple<>(leftValue, rightValue);
    }

    /**
     * Synonym for {@link #valueOf(Object, Object)} method. Its intent is to mimic use of case classes in Scala, which
     * you can do by doing static import of {@code Tuple.Tuple} like so
     * <pre>
     *     import static xyz.devfortress.functional.pebbles.Tuple.Tuple;
     *
     *     Tuple<Integer, String> x = Tuple(1, "Foo");
     * </pre>
     */
    @SuppressWarnings("MethodNameSameAsClassName")
    public static <A, B> Tuple<A, B> Tuple(A leftValue, B rightValue) {
        return new Tuple<>(leftValue, rightValue);
    }
}
