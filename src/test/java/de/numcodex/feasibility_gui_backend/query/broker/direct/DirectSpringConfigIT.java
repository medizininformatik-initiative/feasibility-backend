package de.numcodex.feasibility_gui_backend.query.broker.direct;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(MockitoExtension.class)
public class DirectSpringConfigIT {

  private static final String USERNAME = "some-user-123";
  private static final String PASSWORD = "vALBAi95WW84x3";
  MockWebServer mockWebServer;

  private DirectSpringConfig directSpringConfig;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  void testDirectWebClientFlare_withCredentials() throws InterruptedException {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("Foo"));
    directSpringConfig = new DirectSpringConfig(true, String.format("http://localhost:%s", mockWebServer.getPort()), null, USERNAME, PASSWORD);
    var authHeaderValue = "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));

    WebClient webClient = directSpringConfig.directWebClientFlare();

    webClient
        .get()
        .uri("/foo")
        .retrieve()
        .bodyToMono(String.class)
        .subscribe(responseBody -> {
        })
    ;
    var recordedRequest = mockWebServer.takeRequest();
    assertThat(recordedRequest.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo(authHeaderValue);
  }

  @Test
  void testDirectWebClientFlare_withoutCredentials() throws InterruptedException {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("Foo"));
    directSpringConfig = new DirectSpringConfig(true, String.format("http://localhost:%s", mockWebServer.getPort()), null, null, null);

    WebClient webClient = directSpringConfig.directWebClientFlare();

    webClient
        .get()
        .uri("/foo")
        .retrieve()
        .bodyToMono(String.class)
        .subscribe(responseBody -> {
        })
    ;
    var recordedRequest = mockWebServer.takeRequest();
    assertThat(recordedRequest.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
  }

}
