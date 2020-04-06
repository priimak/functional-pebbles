package xyz.devfortress.functional.pebbles;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import java.rmi.AccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestTry {
    @Test
    public void test() throws Throwable {
        Try<String> successHello = Try.eval(() -> "Hello");
        assertThat(successHello.isSuccess()).isTrue();
        assertThat(successHello.isFailure()).isFalse();
        assertThat(successHello.get()).isEqualTo("Hello");
        Try<String> failureAccess = successHello.andThen(x -> {
            throw new AccessException("");
        });
        assertThat(failureAccess.isSuccess()).isFalse();
        assertThat(failureAccess.isFailure()).isTrue();
        assertThatThrownBy(failureAccess::get).isInstanceOf(AccessException.class);
        Try<String> failureAgain = failureAccess.flatMap($ -> Try.success("Hmm"));
        assertThat(failureAccess.isFailure()).isTrue();
        assertThatThrownBy(failureAgain::get).isInstanceOf(AccessException.class);
        Try<String> failureAndAgain = failureAccess.map($ -> "Hmm");
        assertThat(failureAndAgain.isFailure()).isTrue();
        assertThatThrownBy(failureAndAgain::get).isInstanceOf(AccessException.class);

        Try<String> successHelloWorld = successHello.flatMap(x -> Try.success(x + " World"));
        assertThat(successHelloWorld.isSuccess()).isTrue();
        assertThat(successHelloWorld.isFailure()).isFalse();

        assertThat(
            successHello.<String>visit(
                value -> value.getClass().getSimpleName(),
                error -> error.getClass().getSimpleName()
            )
        ).isEqualTo("String");

        assertThat(
            failureAccess.<String>visit(
                value -> value.getClass().getSimpleName(),
                error -> error.getClass().getSimpleName()
            )
        ).isEqualTo("AccessException");

        // verify that runtime exceptions are not caught
        Assertions.assertThatThrownBy(() -> Try.eval(() -> {
            throw new IllegalArgumentException();
        })).isInstanceOf(IllegalArgumentException.class);

        // verify that Error exceptions are not caught
        Assertions.assertThatThrownBy(() -> Try.eval(() -> {
            throw new AbstractMethodError();
        })).isInstanceOf(AbstractMethodError.class);
    }
}
