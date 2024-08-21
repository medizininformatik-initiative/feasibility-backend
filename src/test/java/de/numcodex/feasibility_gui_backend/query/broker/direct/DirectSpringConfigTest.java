package de.numcodex.feasibility_gui_backend.query.broker.direct;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import de.numcodex.feasibility_gui_backend.query.broker.BrokerClient;
import de.numcodex.feasibility_gui_backend.query.broker.OAuthInterceptor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
    classes = DirectSpringConfig.class
)
class DirectSpringConfigTest {

  @Mock
  private WebClient webClient;

  private final FhirContext fhirContext = FhirContext.forR4();

  @Mock()
  private FhirConnector fhirConnector;

  @Mock
  private FhirHelper fhirHelper;

  private DirectSpringConfig directSpringConfig;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void directWebClientFlare_withCredentials() {
    directSpringConfig = new DirectSpringConfig(true, "http://my.flare.url", null, "username", "password", null, null,
              null);

    WebClient webClient = directSpringConfig.directWebClientFlare();

    assertNotNull(webClient);
    // Since there is no way to check whether the webclient has an auth filter set, see DirectSpringConfigIT.java for a check for that
  }

  @Test
  void directWebClientFlare_withoutCredentials() {
    directSpringConfig = new DirectSpringConfig(true, "http://my.flare.url", null, null, null, null, null, null);

    WebClient webClient = directSpringConfig.directWebClientFlare();

    assertNotNull(webClient);
    // Since there is no way to check whether the webclient has an auth filter set, see DirectSpringConfigIT.java for a check for that
  }

  @Test
  void getFhirClient_withCredentials() {
    directSpringConfig = new DirectSpringConfig(true, null, "http://my.fhir.url", "username", "password", null, null,
              null);

    IGenericClient fhirClient = directSpringConfig.getFhirClient(fhirContext);

    assertNotNull(fhirClient);
    Assertions.assertThat(fhirClient.getInterceptorService().getAllRegisteredInterceptors())
        .anySatisfy(interceptor -> Assertions.assertThat(interceptor).isInstanceOf(BasicAuthInterceptor.class));
  }

  @Test
  void getFhirClient_withoutCredentials() {
    directSpringConfig = new DirectSpringConfig(true, null, "http://my.fhir.url", null, null, null, null, null);

    IGenericClient fhirClient = directSpringConfig.getFhirClient(fhirContext);

    assertNotNull(fhirClient);
    Assertions.assertThat(fhirClient.getInterceptorService().getAllRegisteredInterceptors())
        .noneSatisfy(interceptor -> Assertions.assertThat(interceptor).isInstanceOf(BasicAuthInterceptor.class));
  }

  @Test
  void directBrokerClient_withOAuthCredentials() {
    directSpringConfig = new DirectSpringConfig(true, null, "http://my.fhir.url", null, null, "http://my.oauth.url",
            "foo", "bar");

    IGenericClient fhirClient = directSpringConfig.getFhirClient(fhirContext);

    assertNotNull(fhirClient);
    Assertions.assertThat(fhirClient.getInterceptorService().getAllRegisteredInterceptors())
            .anySatisfy(interceptor -> Assertions.assertThat(interceptor).isInstanceOf(OAuthInterceptor.class));
  }

  @Test
  void directBrokerClient_useCql() {
    directSpringConfig = new DirectSpringConfig(true, null, null, null, null, null, null, null);

    BrokerClient brokerClient = directSpringConfig.directBrokerClient(webClient, false, fhirConnector, fhirHelper);

    assertInstanceOf(DirectBrokerClientCql.class, brokerClient);
  }

  @Test
  void directBrokerClient_useFlare() {
    directSpringConfig = new DirectSpringConfig(false, null, null, null, null, null, null, null);

    BrokerClient brokerClient = directSpringConfig.directBrokerClient(webClient, false, fhirConnector, fhirHelper);

    assertInstanceOf(DirectBrokerClientFlare.class, brokerClient);
  }

}
