package xyz.devfortress.functional.pebbles;

import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static xyz.devfortress.functional.pebbles.Try.Success;

public class TestTryFunction {
    @Test
    public void testMap() throws MalformedURLException {
        TryFunction<String, URI> uriMaker = Try.lift(URI::create);
        TryFunction<String, URL> urlMaker = uriMaker.map(URI::toURL);

        assertThat(urlMaker.apply("http://com.com")).isEqualTo(Success(URI.create("http://com.com").toURL()));
        assertThat(urlMaker.apply("http://c om.com").isFailure()).isTrue();
    }

    @Test
    public void testFlatMap() throws MalformedURLException {
        TryFunction<String, URI> uriMaker = Try.lift(URI::create);
        TryFunction<URI, URL> uri2url = Try.lift(URI::toURL);
        TryFunction<String, URL> urlMaker = uriMaker.flatMap(uri2url::apply);

        assertThat(urlMaker.apply("http://com.com")).isEqualTo(Success(URI.create("http://com.com").toURL()));
        assertThat(urlMaker.apply("http://c om.com").isFailure()).isTrue();
    }
}
