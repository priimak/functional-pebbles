package xyz.devfortress.functional.pebbles;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static xyz.devfortress.functional.pebbles.Collectionz.zipWithIndex;
import static xyz.devfortress.functional.pebbles.Tuple.Tuple;

public class TestCollectionz {
    @Test
    public void test() {
        assertThat(zipWithIndex(Arrays.asList("a", "b", "c")).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(Tuple("a", 0), Tuple("b", 1), Tuple("c", 2)));
    }
}
