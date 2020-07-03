package xyz.devfortress.functional.pebbles;

import org.assertj.core.api.Condition;
import org.assertj.core.api.ThrowableAssert;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationTargetException;
import java.rmi.AccessException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static xyz.devfortress.functional.pebbles.Try.Failure;
import static xyz.devfortress.functional.pebbles.Try.Success;
import static xyz.devfortress.functional.pebbles.Try.Try;
import static xyz.devfortress.functional.pebbles.Tuple.Tuple;

public class TestTry {
    @Test
    public void test() throws Throwable {
        TryVisitor<String, String, String> tryVisitor = new TryVisitor<String, String, String>() {
            @Override
            public String visitSuccess(String value, String context) {
                return "Success:: " + value + " " + context;
            }

            @Override
            public String visitFailure(Throwable error, String context) {
                return "Failure:: " + error.getClass().getSimpleName() + " " + context;
            }
        };

        Try<String> successHello = Try(() -> "Hello");
        assertThat(successHello.isSuccess()).isTrue();
        assertThat(successHello.isFailure()).isFalse();
        assertThat(successHello.get()).isEqualTo("Hello");
        assertThat(successHello.toOptional()).isEqualTo(Optional.of("Hello"));
        assertThat(successHello.recoverWith($ -> Success("Odd man out")).toOptional()).isEqualTo(Optional.of("Hello"));
        assertThat(successHello.toString()).isEqualTo("Success(Hello)");
        assertThat(successHello.visit(tryVisitor, "World!")).isEqualTo("Success:: Hello World!");
        assertThat(successHello.getOrElse("Aloha")).isEqualTo("Hello");
        assertThat(successHello.getOrElse(() -> "Aloha")).isEqualTo("Hello");
        assertThat(successHello.orElse(() -> Success("Aloha"))).isEqualTo(Success("Hello"));
        assertThat(successHello.orElse(Failure(new IllegalStateException()))).isEqualTo(Success("Hello"));
        assertThat(successHello.map(value -> "Aloha")).isEqualTo(Success("Aloha"));
        assertThat(successHello.<String>fold(error -> "error", success -> "Great success")).isEqualTo("Great success");

        AtomicReference<String> acc = new AtomicReference<>("");
        successHello.accept(success -> acc.set("success"), error -> acc.set("error"));
        assertThat(acc.get()).isEqualTo("success");

        Try<String> failedHello = successHello.failed();
        assertThat(failedHello.isFailure()).isTrue();
        assertThat(failedHello.<Boolean>transform(
            success -> false,
            error -> error instanceof UnsupportedOperationException && error.getMessage().equals("Success.failed")
        )).isTrue();
        assertThat(failedHello.getOrElse("Aloha")).isEqualTo("Aloha");
        assertThat(failedHello.getOrElse(() -> "Aloha")).isEqualTo("Aloha");
        assertThat(failedHello.orElse(() -> Success("Aloha")).isSuccess()).isTrue();
        assertThat(failedHello.failed() == failedHello).isTrue();
        assertThat(failedHello.recover(error -> "All clear")).isEqualTo(Success("All clear"));
        assertThat(failedHello.orElse(Success("Mahala"))).isEqualTo(Success("Mahala"));

        acc.set("");
        failedHello.accept(success -> acc.set("success"), error -> acc.set("error"));
        assertThat(acc.get()).isEqualTo("error");

        acc.set("");
        successHello.forEach(acc::set);
        assertThat(acc.get()).isEqualTo("Hello");

        Try<String> failureAccess = successHello.andThen(x -> {
            throw new AccessException("");
        });
        assertThat(failureAccess.isSuccess()).isFalse();
        assertThat(failureAccess.isFailure()).isTrue();
        assertThat(failureAccess.toOptional()).isEmpty();
        assertThat(failureAccess.recoverWith(error2 -> Success("Odd man out")).toOptional())
            .isEqualTo(Optional.of("Odd man out"));
        assertThat(failureAccess.toString()).isEqualTo("Failure(AccessException)");
        assertThat(failureAccess.visit(tryVisitor, "foo")).isEqualTo("Failure:: AccessException foo");

        acc.set("NOOP");
        failureAccess.forEach(acc::set);
        assertThat(acc.get()).isEqualTo("NOOP");

        assertThatThrownBy(failureAccess::get).isInstanceOf(AccessException.class);
        Try<String> failureAgain = failureAccess.flatMap($ -> Try.success("Hmm"));
        assertThat(failureAccess.isFailure()).isTrue();
        assertThatThrownBy(failureAgain::get).isInstanceOf(AccessException.class);
        Try<String> failureAndAgain = failureAccess.map($ -> "Hmm");
        assertThat(failureAndAgain.isFailure()).isTrue();
        assertThatThrownBy(failureAndAgain::get).isInstanceOf(AccessException.class);

        assertThat(
            Try.ofOptional(Optional.empty())
                .<Boolean>transform($ -> false, error1 -> error1 instanceof NoSuchElementException)
        ).isTrue();

        Try<String> successHelloWorld = successHello.flatMap(x -> Success(x + " World"));
        assertThat(successHelloWorld.isSuccess()).isTrue();
        assertThat(successHelloWorld.isFailure()).isFalse();

        Try<String> failedHelloWorld = successHello.flatMap(x -> Failure(new Exception()));
        assertThat(failedHelloWorld.isSuccess()).isFalse();
        assertThat(failedHelloWorld.isFailure()).isTrue();

        assertThat(
            successHello.<String>transform(
                value -> value.getClass().getSimpleName(),
                error -> error.getClass().getSimpleName()
            )
        ).isEqualTo("String");

        assertThat(
            failureAccess.<String>transform(
                value -> value.getClass().getSimpleName(),
                error -> error.getClass().getSimpleName()
            )
        ).isEqualTo("AccessException");

        ThrowingFunction<Tuple<Integer, Integer>, Integer> div = nums -> nums._1 / nums._2;
        assertThatThrownBy(() -> div.apply(Tuple(7, 0))).isInstanceOf(ArithmeticException.class);

        Function<Tuple<Integer, Integer>, Try<Integer>> safeDiv = Try.lift(div);
        assertThat(safeDiv.apply(Tuple(7, 0)).isFailure());
        assertThat(safeDiv.apply(Tuple(7, 3))).isEqualTo(Success(2));
    }

    @Test
    public void testEquality() {
        Try<String> helloSuccess = Success("Hello");
        assertThat(helloSuccess.equals(helloSuccess)).isTrue();
        assertThat(helloSuccess).isEqualTo(Success("Hello"));
        assertThat(helloSuccess.equals(null)).isFalse();
        assertThat(helloSuccess.equals("Hello")).isFalse();

        Try<Object> totalFailure = Failure(new Exception("Total failure"));
        assertThat(totalFailure.equals(totalFailure)).isTrue();
        assertThat(totalFailure.equals(Failure(new Exception("Total failure")))).isFalse();
        assertThat(totalFailure.equals(null)).isFalse();
        assertThat(totalFailure.equals("Total failure")).isFalse();
    }

    @Test
    public void testRecover() {
        assertThat(Success("Hello").recover(error -> "Aloha")).isEqualTo(Success("Hello"));
        assertThat(Success("Hello").recoverWith(error -> Success("Aloha"))).isEqualTo(Success("Hello"));

        assertThat(Failure(new Exception("Hello")).recover(error -> "Aloha")).isEqualTo(Success("Aloha"));
        assertThat(Failure(new Exception("Hello")).recoverWith(error -> Success("Aloha"))).isEqualTo(Success("Aloha"));
    }

    @Test
    public void testExceptions()
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        List<Class<? extends Throwable>> classes =
            Arrays.asList(OutOfMemoryError.class, ThreadDeath.class, LinkageError.class);
        for (Class<? extends Throwable> errorClass : classes) {
            Throwable errorToTest = errorClass.getConstructor().newInstance();
            assertThatThrownBy(() -> Try.eval(() -> {
                throw errorToTest;
            })).isInstanceOf(errorClass);

            assertThatThrownBy(() -> Try(() -> {
                throw errorToTest;
            })).isInstanceOf(errorClass);

            assertThatThrownBy(() -> Success(Unit.INSTANCE).map($ -> {
                throw errorToTest;
            })).isInstanceOf(errorClass);

            assertThatThrownBy(() -> Success(Unit.INSTANCE).fold(error -> "", $ -> {
                throw errorToTest;
            })).isInstanceOf(errorClass);

            assertThatThrownBy(() -> Success(Unit.INSTANCE).filter($ -> {
                throw errorToTest;
            })).isInstanceOf(errorClass);

            assertThatThrownBy(() -> Success(Unit.INSTANCE).flatMap($ -> {
                throw errorToTest;
            })).isInstanceOf(errorClass);

            assertThatThrownBy(() -> Failure(new Exception()).recover($ -> {
                throw errorToTest;
            })).isInstanceOf(errorClass);

            assertThatThrownBy(() -> Failure(new Exception()).recoverWith($ -> {
                throw errorToTest;
            })).isInstanceOf(errorClass);

            assertThatThrownBy(() -> Failure(new Exception()).orElse(() -> {
                throw errorToTest;
            })).isInstanceOf(errorClass);
        }

        List<ThrowableAssert.ThrowingCallable> fs = Arrays.asList(
            () -> Success(Unit.INSTANCE).map($ -> {
                throw new InterruptedException();
            }),
            () -> Success(Unit.INSTANCE).filter($ -> {
                throw new InterruptedException();
            }),
            () -> Success(Unit.INSTANCE).fold(error -> "", $ -> {
                throw new InterruptedException();
            }),
            () -> Failure(new Exception()).recover($ -> {
                throw new InterruptedException();
            }),
            () -> Failure(new Exception()).recoverWith($ -> {
                throw new InterruptedException();
            }),
            () -> Failure(new Exception()).orElse(() -> {
                throw new InterruptedException();
            }),
            () -> Try.eval(() -> {
                throw new InterruptedException();
            }),
            () -> Try(() -> {
                throw new InterruptedException();
            }),
            () -> Success(Unit.INSTANCE).flatMap($ -> {
                throw new InterruptedException();
            }));
        for (ThrowableAssert.ThrowingCallable f : fs) {
            assertThatThrownBy(f).isInstanceOf(RuntimeException.class)
                .is(new Condition<>(
                    ex -> ex.getCause() instanceof InterruptedException,
                    "InterruptedException is wrapped into RuntimeException"
                ));
        }

        assertThat(Success(Unit.INSTANCE).map($ -> {
            throw new RuntimeException();
        }).isFailure()).isTrue();
        assertThat(Try.eval(() -> {
            throw new RuntimeException();
        }).isFailure()).isTrue();
        assertThat(Success(Unit.INSTANCE).flatMap($ -> {
            throw new RuntimeException();
        }).isFailure()).isTrue();
        assertThat(Success(Unit.INSTANCE).filter($ -> {
            throw new RuntimeException();
        }).isFailure()).isTrue();
        assertThat(Failure(new Exception()).recover($ -> {
            throw new RuntimeException();
        }).isFailure()).isTrue();
        assertThat(Failure(new Exception()).recoverWith($ -> {
            throw new RuntimeException();
        }).isFailure()).isTrue();
        assertThat(Success(Unit.INSTANCE)
            .<String>fold(error -> error.getClass().getSimpleName(), value -> {
                throw new IllegalStateException();
            })).isEqualTo("IllegalStateException");
        assertThat(Failure(new Exception()).orElse(() -> {
            throw new RuntimeException();
        }).isFailure()).isTrue();
    }

    @Test
    public void testHashCode() {
        assertThat(Success("Hello").hashCode()).isEqualTo(Success("Hello").hashCode());
        assertThat(Success("Hello").hashCode()).isNotEqualTo(Success("Aloha").hashCode());
    }

    @Test
    public void testOfNullable() {
        Try<?> nullDerivedTry = Try.ofNullable(null);
        assertThat(nullDerivedTry.isFailure()).isTrue();
        assertThat(nullDerivedTry.isSuccess()).isFalse();
        Throwable fold = nullDerivedTry.fold(Function.identity(), $ -> new Exception());
        assertThat(fold)
            .isInstanceOf(NoSuchElementException.class)
            .is(new Condition<>(
                ex -> ex.getMessage().equals("No value present"),
                "ofNullable(null) results in Failure(NoSuchElementException)"
            ));

        Try<String> aloha = Try.ofNullable("Aloha");
        assertThat(aloha.isSuccess()).isTrue();
        assertThat(aloha.transform(Function.identity(), error -> "error")).isEqualTo("Aloha");
    }

    @Test
    public void testFilter() {
        assertThat(Try.eval(() -> 7).filter(x -> x < 8)).isEqualTo(Success(7));
        assertThat(Try.success(7).filter(x -> x > 8).isFailure());
        assertThat(
            Success(7).filter(x -> x > 8)
                .<String>transform(
                    success -> "",
                    error -> error.getClass().getSimpleName() + "(" + error.getMessage() + ")"
                )
        ).isEqualTo("NoSuchElementException(Predicate does not hold for 7)");
        assertThat(
            Try.failure(new NullPointerException(""))
                .filter($ -> true)
                .<String>transform(
                    success -> "",
                    error -> error.getClass().getSimpleName() + "(" + error.getMessage() + ")"
                )
        ).isEqualTo("NullPointerException()");
    }

    @Test
    public void testCollectors() {
        List<Try<Integer>> list = Arrays.asList(Success(7), Failure(new IllegalStateException()), Success(8));
        assertThat(list.stream().collect(Try.valuesCollector())).isEqualTo(Arrays.asList(7, 8));

        Tuple<List<Integer>, List<Throwable>> result = list.stream().collect(Try.partition());
        assertThat(result._1).isEqualTo(Arrays.asList(7, 8));
        assertThat(result._2.size()).isEqualTo(1);
        assertThat(result._2.get(0)).isInstanceOf(IllegalStateException.class);
    }
}
