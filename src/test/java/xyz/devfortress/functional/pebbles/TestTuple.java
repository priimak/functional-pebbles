package xyz.devfortress.functional.pebbles;

import org.testng.annotations.Test;

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
    }
}
