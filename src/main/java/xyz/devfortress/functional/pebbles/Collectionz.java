package xyz.devfortress.functional.pebbles;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Collectionz {
    private Collectionz() {
        throw new AssertionError();
    }

    /**
     * Zips provided iterable with index of each element in it forming a stream of tuples each containing
     * original value in the iterable and its index starting from 0.
     * <p>For example</p>
     * <pre>
     * List&lt;String&gt; strings = Arrays.asList("a", "b", "c")
     * List&lt;Tuple&lt;String, Integer&gt;&gt; res = Collectionz.zipWithIndex(strings)
     * </pre>
     * Returns
     * <pre>
     * [("a", 0), ("b", 1), ("c", 2)]
     * </pre>
     */
    public static <T> Stream<Tuple<T, Integer>> zipWithIndex(Iterable<T> iterable) {
        Iterator<T> iterator = iterable.iterator();
        return iterateFromZero(iterator::hasNext)
            .mapToObj(i -> new Tuple<>(iterator.next(), i));
    }

    private static IntStream iterateFromZero(Supplier<Boolean> hasNext) {
        Spliterator.OfInt spliterator = new Spliterators.AbstractIntSpliterator(
            Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL) {
            int nextVal = 0;

            @Override
            public boolean tryAdvance(IntConsumer action) {
                return hasNext.get();
            }

            @Override
            public void forEachRemaining(IntConsumer action) {
                while (hasNext.get()) {
                    action.accept(nextVal);
                    nextVal++;
                }
            }
        };

        return StreamSupport.intStream(spliterator, false);
    }
}
