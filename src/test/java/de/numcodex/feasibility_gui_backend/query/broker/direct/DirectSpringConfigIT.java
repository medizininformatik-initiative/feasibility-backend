package de.numcodex.feasibility_gui_backend.query.broker.direct;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.google.common.base.Charsets;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import okhttp3.Headers;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ExtendWith(MockitoExtension.class)
public class DirectSpringConfigIT {

  @Container
  public static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:25.0")
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
  void testDirectWebClientFlare_withCredentials() throws InterruptedException {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("Foo"));
    directSpringConfig = new DirectSpringConfig(true, String.format("http://localhost:%s", mockWebServer.getPort()),
        null, USERNAME, PASSWORD, null, null, null, 10000, 10000, 10000);
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
  void testDirectWebClientFlare_withoutCredentials() throws InterruptedException {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("Foo"));
    directSpringConfig = new DirectSpringConfig(true, String.format("http://localhost:%s", mockWebServer.getPort()),
        null, null, null, null, null, null, 10000, 10000, 10000);

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
  void directWebClientFlare_withOAuthCredentials() throws InterruptedException, IOException {
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
        10000, 10000, 10000);
    IGenericClient client = directSpringConfig.getFhirClient(FhirContext.forR4());

    client.capabilities().ofType(CapabilityStatement.class).execute();

    var recordedRequest = mockWebServer.takeRequest();
    assertThat(recordedRequest.getHeader(HttpHeaders.AUTHORIZATION)).startsWith("Bearer ey");
  }
}
