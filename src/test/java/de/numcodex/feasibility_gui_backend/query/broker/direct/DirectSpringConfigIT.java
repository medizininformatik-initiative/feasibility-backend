package de.numcodex.feasibility_gui_backend.query.broker.direct;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.google.common.base.Charsets;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.netty.handler.timeout.ReadTimeoutException;
import okhttp3.Headers;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@ExtendWith(MockitoExtension.class)
public class DirectSpringConfigIT {

  @Container
  public static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.2")
      .withAdminUsername("admin")
      .withAdminPassword("admin")
      .withRealmImportFile(new ClassPathResource("realm-test.json", DirectSpringConfigIT.class).getPath())
      .withReuse(true);

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
  @DisplayName("Direct broker FLARE webclient request with basic authentication")
  void flareClientWithCredentials() throws InterruptedException {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("Foo"));
    directSpringConfig = new DirectSpringConfig(true, String.format("http://localhost:%s", mockWebServer.getPort()),
            null, USERNAME, PASSWORD, null, null, null, Duration.ofSeconds(10));
    var authHeaderValue = "Basic "
        + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));

    WebClient webClient = directSpringConfig.directWebClientFlare();

    webClient
        .get()
        .uri("/foo")
        .retrieve()
        .bodyToMono(String.class)
        .subscribe(responseBody -> {
        });
    var recordedRequest = mockWebServer.takeRequest();
    assertThat(recordedRequest.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo(authHeaderValue);
  }

  @Test
  @DisplayName("Direct broker FLARE webclient request with no authentication")
  void flareClientWithoutCredentials() throws InterruptedException {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("Foo"));
    directSpringConfig = new DirectSpringConfig(true, String.format("http://localhost:%s", mockWebServer.getPort()),
            null, null, null, null, null, null, Duration.ofSeconds(10));

    WebClient webClient = directSpringConfig.directWebClientFlare();

    webClient
        .get()
        .uri("/foo")
        .retrieve()
        .bodyToMono(String.class)
        .subscribe(responseBody -> {
        });
    var recordedRequest = mockWebServer.takeRequest();
    assertThat(recordedRequest.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
  }

  @Test
  @DisplayName("Direct broker FHIR client request with OAuth token")
  void fhirClientWithOAuthCredentials() throws InterruptedException, IOException {
    String metadata = new ClassPathResource("fhir-metadata.json", DirectSpringConfigIT.class)
        .getContentAsString(Charsets.UTF_8);
    mockWebServer.setDispatcher(new Dispatcher() {

      @Override
      public MockResponse dispatch(RecordedRequest arg0) throws InterruptedException {
        if ("/metadata".equals(arg0.getPath())) {
          return new MockResponse().setResponseCode(200)
              .setHeaders(Headers.of("Content-Type", "application/fhir+json"))
              .setBody(metadata);
        } else {
          return new MockResponse().setResponseCode(404);
        }
      }
    });
    directSpringConfig = new DirectSpringConfig(true, null,
        String.format("http://localhost:%s", mockWebServer.getPort()), null, null,
            String.format("http://localhost:%s/realms/test", keycloak.getFirstMappedPort()), "account", "test",
            Duration.ofSeconds(10));
    IGenericClient client = directSpringConfig.getFhirClient(FhirContext.forR4());

    client.capabilities().ofType(CapabilityStatement.class).execute();

    var recordedRequest = mockWebServer.takeRequest();
    assertThat(recordedRequest.getHeader(HttpHeaders.AUTHORIZATION)).startsWith("Bearer ey");
  }

  @Test
  @DisplayName("flare webClient succeeds getting 1s delayed response before timeout of 5s")
  void flareClientSucceedsFinishingBeforeTimeout() throws Exception {
      var timeout = Duration.ofSeconds(5);
      var body = "Foo";
      mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(body).setBodyDelay(1, SECONDS));
      directSpringConfig = new DirectSpringConfig(false, String.format("http://localhost:%s", mockWebServer.getPort()),
              null, null, null, null, null, null, timeout);
      var client = directSpringConfig.directWebClientFlare();

      Instant start = Instant.now();
      var response = client.get()
              .uri("/foo")
              .retrieve()
              .bodyToMono(String.class)
              .block();

      assertThat(response).isEqualTo(body);
      assertThat(Duration.between(start, Instant.now())).isLessThan(timeout);
  }

  @Test
  @DisplayName("flare webClient fails not getting 5s delayed response before given timeout of 2s")
  void flareClientFailsReachingTimeout() throws Exception {
      var timeout = Duration.ofSeconds(2);
      var delta = Duration.ofSeconds(1);
      var delay = 5;
      mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("Foo").setBodyDelay(delay, SECONDS));
      directSpringConfig = new DirectSpringConfig(false, String.format("http://localhost:%s", mockWebServer.getPort()),
              null, null, null, null, null, null, timeout);
      var client = directSpringConfig.directWebClientFlare();

      Instant start = Instant.now();
      assertThatThrownBy(() -> client.get()
              .uri("/foo")
              .retrieve()
              .bodyToMono(String.class)
              .block()).isInstanceOf(WebClientResponseException.class)
                      .hasCauseInstanceOf(ReadTimeoutException.class);
      var elapsed = Duration.between(start, Instant.now());
      assertThat(elapsed).isBetween(timeout, timeout.plus(delta));
      Thread.sleep(Duration.ofSeconds(delay).minus(elapsed).toMillis());
  }

  @Test
  @DisplayName("FHIR client succeeds getting 1s delayed response before given timeout of 5s")
  void fhirClientSucceedsFinishingBeforeTimeout() throws Exception {
      var metadata = new ClassPathResource("fhir-metadata.json", DirectSpringConfigIT.class)
              .getContentAsString(Charsets.UTF_8);
      var timeout = Duration.ofSeconds(5);
      var delta = Duration.ofSeconds(1);
      var delay = 1;
      var response = new MockResponse().setResponseCode(200)
              .setHeaders(Headers.of("Content-Type", "application/fhir+json"))
              .setBody(metadata);
      mockWebServer.enqueue(response);
      mockWebServer.enqueue(response.clone()
              .setBodyDelay(delay, SECONDS));
      directSpringConfig = new DirectSpringConfig(true, null,
              String.format("http://localhost:%s", mockWebServer.getPort()), null, null, null, null, null, timeout);
      var client = directSpringConfig.getFhirClient(FhirContext.forR4());

      Instant start = Instant.now();
      client.capabilities().ofType(CapabilityStatement.class).execute();

      assertThat(Duration.between(start, Instant.now())).isLessThan(timeout.plus(delta));
      assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
      assertThat(mockWebServer.takeRequest().getPath())
              .isEqualTo(mockWebServer.takeRequest().getPath())
              .isEqualTo("/metadata");
  }

  @Test
  @DisplayName("FHIR client fails not getting 5s delayed response before given timeout of 2s")
  @Disabled("This test frequently fails when run via github actions. Locally, it runs without trouble. Further investigation recommended.")
  void fhirClientFailsReachingTimeout() throws Exception {
      var metadata = new ClassPathResource("fhir-metadata.json", DirectSpringConfigIT.class)
              .getContentAsString(Charsets.UTF_8);
      var timeout = Duration.ofSeconds(2);
      var delta = Duration.ofSeconds(1);
      var delay = 5;
      var response = new MockResponse().setResponseCode(200)
              .setHeaders(Headers.of("Content-Type", "application/fhir+json"))
              .setBody(metadata);
      mockWebServer.enqueue(response);
      mockWebServer.enqueue(response.clone()
              .setBodyDelay(delay, SECONDS));
      directSpringConfig = new DirectSpringConfig(true, null,
              String.format("http://localhost:%s", mockWebServer.getPort()), null, null, null, null, null, timeout);
      var client = directSpringConfig.getFhirClient(FhirContext.forR4());

      Instant start = Instant.now();
      assertThatThrownBy(() -> client.capabilities().ofType(CapabilityStatement.class).execute())
              .isInstanceOf(FhirClientConnectionException.class);
      var elapsed = Duration.between(start, Instant.now());
      assertThat(elapsed).isBetween(timeout, timeout.plus(delta));
      assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
      assertThat(mockWebServer.takeRequest().getPath())
              .isEqualTo(mockWebServer.takeRequest().getPath())
              .isEqualTo("/metadata");
      Thread.sleep(Duration.ofSeconds(delay).minus(elapsed).toMillis());
  }
}
