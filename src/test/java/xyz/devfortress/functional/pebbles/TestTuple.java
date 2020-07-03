package xyz.devfortress.functional.pebbles;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static xyz.devfortress.functional.pebbles.Tuple.Tuple;

public class TestTuple {
    @Test
    public void test() {
        Tuple<Integer, String> x = Tuple(1, "Foo");
        assertThat(x.equals(x)).isTrue();
        assertThat(x).isEqualTo(Tuple.valueOf(1, "Foo"));
        assertThat(x).isNotEqualTo(new Tuple<>(2, "Foo"));
        assertThat(x).isNotEqualTo(new Tuple<>(1, "Moo"));
        assertThat(x).isNotEqualTo("Moo");
        assertThat(x).isNotEqualTo(null);
        assertThat(x._1).isEqualTo(1);
        assertThat(x._2).isEqualTo("Foo");
        assertThat(Tuple.left(x)).isEqualTo(1);
        assertThat(Tuple.right(x)).isEqualTo("Foo");

        AtomicReference<String> acc = new AtomicReference<>("");
        x.forEach(integer -> acc.set(acc.get() + integer), string -> acc.set(acc.get() + string));
        assertThat(acc.get()).isEqualTo("1Foo");

        assertThat(x.map(Arrays::asList, Optional::of))
            .isEqualTo(Tuple(Collections.singletonList(1), Optional.of("Foo")));

        assertThat(x.flatMap((i, s) -> Tuple(s, i))).isEqualTo(Tuple("Foo", 1));
        assertThat(x.<String>transform((i, s) -> s + " = " + i)).isEqualTo("Foo = 1");

        assertThat(x.toString()).isEqualTo("(1, Foo)");

        assertThat(x.hashCode()).isEqualTo(Tuple(1, "Foo").hashCode());
        assertThat(x.hashCode()).isNotEqualTo(Tuple(2, "Foo").hashCode());
        assertThat(x.hashCode()).isNotEqualTo(Tuple(1, "Oof").hashCode());
    }
}
