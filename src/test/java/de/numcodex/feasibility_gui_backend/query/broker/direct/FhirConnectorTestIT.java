package de.numcodex.feasibility_gui_backend.query.broker.direct;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.io.IOException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class FhirConnectorTestIT {

  private final FhirContext fhirContext = FhirContext.forR4();

  private FhirConnector fhirConnector;
  private FhirHelper fhirHelper;

  private final GenericContainer<?> blaze = new GenericContainer<>(
      DockerImageName.parse("samply/blaze:0.18"))
      .withImagePullPolicy(PullPolicy.alwaysPull())
      .withExposedPorts(8080)
      .waitingFor(Wait.forHttp("/health").forStatusCodeMatching(c -> c >= 200 && c <= 500))
      .withStartupAttempts(3);

  @BeforeAll
  void setUp() {
    blaze.start();
    fhirContext.getRestfulClientFactory().setSocketTimeout(200 * 1000);
    IGenericClient fhirClient = fhirContext.newRestfulGenericClient(
        format("http://localhost:%d/fhir", blaze.getFirstMappedPort()));
    fhirConnector = new FhirConnector(fhirClient);
    fhirHelper = new FhirHelper(fhirContext);
  }

  @AfterAll
  void tearDown() {
    blaze.stop();
  }

  @Test
  void testCreateBundle() throws IOException {
    var cqlString = FhirHelper.getResourceFileAsString("gender-male.cql");
    assertThat(
        fhirHelper.createBundle(cqlString, "urn:uuid:foo:bar", "urn:uuid:bar:foo")).isInstanceOf(
        Bundle.class);
  }

  @Test
  void testTransmitBundle() throws IOException {
    var cqlString = FhirHelper.getResourceFileAsString("gender-male.cql");
    Bundle bundle = fhirHelper.createBundle(cqlString, "urn:uuid:foo:bar", "urn:uuid:bar:foo");
    assertDoesNotThrow(() -> fhirConnector.transmitBundle(bundle));
  }

  @Test
  void testTransmitBundleWithoutBundle() {
    assertThrows(NullPointerException.class, () -> fhirConnector.transmitBundle(null));
  }

  @Test
  void testEvaluateMeasure() throws IOException {
    var cqlString = FhirHelper.getResourceFileAsString("gender-male.cql");
    var libraryUri = "urn:uuid:library:1";
    var measureUri = "urn:uuid:measure:1";

    var bundle = fhirHelper.createBundle(cqlString, libraryUri, measureUri);
    assertDoesNotThrow(() -> fhirConnector.transmitBundle(bundle));
    assertThat(fhirConnector.evaluateMeasure(measureUri)).isInstanceOf(MeasureReport.class);
  }

  @Test
  void testEvaluateMeasureWithoutMeasureUri() {
    assertThrows(IOException.class, () -> fhirConnector.evaluateMeasure(null));
  }

  @Test
  void testEvaluateMeasureWithInvalidMeasureUri() {
    assertThrows(IOException.class, () -> fhirConnector.evaluateMeasure("foobar"));
  }

  @Test
  void getResourceFileAsString() throws IOException {
    assertDoesNotThrow(() -> FhirHelper.getResourceFileAsString("gender-male.cql"));
    assertThat(FhirHelper.getResourceFileAsString("gender-male.cql")).isInstanceOf(String.class);
    assertThat(FhirHelper.getResourceFileAsString("gender-male.cql")).contains(
        "Patient.gender = 'male'");
  }

  @Test
  void getResourceFileAsStringFileNotFoundThrows() {
    String nonExistingFilename = "does-not-exist";
    RuntimeException runtimeException = assertThrows(RuntimeException.class,
        () -> FhirHelper.getResourceFileAsString(nonExistingFilename));
    assertEquals("File not found in classpath: " + nonExistingFilename,
        runtimeException.getMessage());
  }
}
