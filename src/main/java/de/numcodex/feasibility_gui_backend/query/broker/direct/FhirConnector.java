package de.numcodex.feasibility_gui_backend.query.broker.direct;

import static de.numcodex.feasibility_gui_backend.query.QueryMediaType.CQL;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION;
import static org.hl7.fhir.r4.model.Bundle.HTTPVerb.POST;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import java.io.IOException;
import java.io.InputStream;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.springframework.stereotype.Component;


@Component
public class FhirConnector {

  private final IGenericClient client;

  public FhirConnector( IGenericClient client) {
    this.client = client;
  }

  /**
   * Submit a {@link Bundle} to the FHIR server.
   * @param bundle the {@link Bundle} to submit
   * @throws IOException if the communication with the FHIR server fails due to any client or server error
   */
  public void transmitBundle(Bundle bundle) throws IOException {
    try {
      client.transaction().withBundle(bundle).execute();
    } catch (BaseServerResponseException e) {
      throw new IOException("An error occurred while trying to create measure and library", e);
    }
  }

  /**
   * Get the {@link MeasureReport} for a previously transmitted {@link Measure}
   * @param measureUri the identifier of the {@link Measure}
   * @return the retrieved {@link MeasureReport} from the server
   * @throws IOException if the communication with the FHIR server fails due to any client or server error
   */
  public MeasureReport evaluateMeasure(String measureUri) throws IOException {
    try {
      return client.operation()
          .onType(Measure.class)
          .named("evaluate-measure")
          .withSearchParameter(Parameters.class, "measure", new StringParam(measureUri))
          .andSearchParameter("periodStart", new DateParam("1900"))
          .andSearchParameter("periodEnd", new DateParam("2100"))
          .useHttpGet()
          .returnResourceType(MeasureReport.class)
          .execute();
    } catch (BaseServerResponseException e) {
      throw new IOException("An error occurred while trying to evaluate a measure report", e);
    }
  }

}
