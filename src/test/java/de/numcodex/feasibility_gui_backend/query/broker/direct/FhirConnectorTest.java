package de.numcodex.feasibility_gui_backend.query.broker.direct;

import static org.junit.jupiter.api.Assertions.assertThrows;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import java.io.IOException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FhirConnectorTest {


  private static final String MEASURE_URI = "uri:1-measure-example-uri";

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  IGenericClient client;


  FhirConnector fhirConnector;

  @BeforeEach
  void setUp() {
    fhirConnector = new FhirConnector(client);
  }

  @Test
  void testTransmitBundleSuccess() throws Exception {
    Bundle bundle = new Bundle();
    BaseServerResponseException e = BaseServerResponseException.newInstance(200, "test");
    Mockito.when(client.transaction().withBundle(bundle).execute()).thenReturn(bundle);

    fhirConnector.transmitBundle(bundle);

    verify(client.transaction().withBundle(bundle)).execute();
  }


  @Test
  void testTransmitBundleIOException() throws Exception {
    Bundle bundle = new Bundle();

    BaseServerResponseException e = BaseServerResponseException.newInstance(200, "test");
    Mockito.when(client.transaction().withBundle(bundle).execute()).thenThrow(e);

    assertThrows(IOException.class, () -> fhirConnector.transmitBundle(bundle));
  }

  @Test
  void testEvaluateMeasureSuccess() throws Exception {
    Bundle bundle = new Bundle();
    BaseServerResponseException e = BaseServerResponseException.newInstance(200, "test");
    MeasureReport measureReport = new MeasureReport();

    Mockito.when(client.operation()
        .onType(Measure.class)
        .named("evaluate-measure")
        .withSearchParameter(Parameters.class, "measure", new StringParam(MEASURE_URI))
        .andSearchParameter("periodStart", new DateParam("1900"))
        .andSearchParameter("periodEnd", new DateParam("2100"))
        .useHttpGet()
        .returnResourceType(MeasureReport.class)
        .execute()).thenReturn(measureReport);

    assertEquals(measureReport, fhirConnector.evaluateMeasure(MEASURE_URI));
    verify(client.operation()
        .onType(Measure.class)
        .named("evaluate-measure")
        .withSearchParameter(Parameters.class, "measure", new StringParam(MEASURE_URI))
        .andSearchParameter("periodStart", new DateParam("1900"))
        .andSearchParameter("periodEnd", new DateParam("2100"))
        .useHttpGet()
        .returnResourceType(MeasureReport.class)
        ).execute();
  }

  @Test
  void testEvaluateMeasureIOException() throws Exception {

    Bundle bundle = new Bundle();
    BaseServerResponseException e = BaseServerResponseException.newInstance(200, "test");
    Mockito.when(client.operation()
        .onType(Measure.class)
        .named("evaluate-measure")
        .withSearchParameter(Parameters.class, "measure", new StringParam(MEASURE_URI))
        .andSearchParameter("periodStart", new DateParam("1900"))
        .andSearchParameter("periodEnd", new DateParam("2100"))
        .useHttpGet()
        .returnResourceType(MeasureReport.class)
        .execute()).thenThrow(e);

    assertThrows(IOException.class, () -> fhirConnector.evaluateMeasure(MEASURE_URI));

  }

}
