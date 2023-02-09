package de.numcodex.feasibility_gui_backend.query.broker.direct;

import static de.numcodex.feasibility_gui_backend.query.QueryMediaType.CQL;
import static de.numcodex.feasibility_gui_backend.query.collect.QueryStatus.COMPLETED;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.numcodex.feasibility_gui_backend.query.broker.QueryDefinitionNotFoundException;
import de.numcodex.feasibility_gui_backend.query.broker.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.query.broker.SiteNotFoundException;
import de.numcodex.feasibility_gui_backend.query.collect.QueryStatusListener;
import de.numcodex.feasibility_gui_backend.query.collect.QueryStatusUpdate;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
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
@SuppressWarnings("NewClassNamingConvention")
class DirectBrokerClientCqlIT {

    private static final int ASYNC_TIMEOUT_WAIT_MS = 2000;
    private static final Long TEST_BACKEND_QUERY_ID = 1L;

    private final GenericContainer<?> blaze = new GenericContainer<>(
        DockerImageName.parse("samply/blaze:0.18"))
        .withImagePullPolicy(PullPolicy.alwaysPull())
        .withExposedPorts(8080)
        .waitingFor(Wait.forHttp("/health").forStatusCodeMatching(c -> c >= 200 && c <= 500))
        .withStartupAttempts(3);

    DirectBrokerClientCql client;
    private final FhirContext fhirContext = FhirContext.forR4();

    @BeforeAll
    void setUp() {
        blaze.start();
        fhirContext.getRestfulClientFactory().setSocketTimeout(200 * 1000);
        IGenericClient fhirClient = fhirContext.newRestfulGenericClient(
            format("http://localhost:%d/fhir", blaze.getFirstMappedPort()));
        FhirConnector fhirConnector = new FhirConnector(fhirClient);
        FhirHelper fhirHelper = new FhirHelper(fhirContext);
        client = new DirectBrokerClientCql(fhirConnector, false, fhirHelper);

        createDummyPatient(fhirClient, Enumerations.AdministrativeGender.MALE, "Curie", "Pierre");
        createDummyPatient(fhirClient, Enumerations.AdministrativeGender.FEMALE, "Curie", "Marie");
        createDummyPatient(fhirClient, Enumerations.AdministrativeGender.MALE, "Zuse", "Konrad");
    }

    @AfterAll
    void tearDown() {
        blaze.stop();
    }

    @Test
    void testExecuteQueryMale()
        throws QueryNotFoundException, IOException, SiteNotFoundException, QueryDefinitionNotFoundException {
        var brokerQueryId = client.createQuery(TEST_BACKEND_QUERY_ID);
        var cqlString = FhirHelper.getResourceFileAsString("gender-male.cql");
        client.addQueryDefinition(brokerQueryId, CQL, cqlString);

        var statusListener = mock(QueryStatusListener.class);
        client.addQueryStatusListener(statusListener);
        client.publishQuery(brokerQueryId);

        var statusUpdate = new QueryStatusUpdate(client, brokerQueryId, "1", COMPLETED);
        verify(statusListener, timeout(ASYNC_TIMEOUT_WAIT_MS)).onClientUpdate(TEST_BACKEND_QUERY_ID, statusUpdate);

        assertThat(client.getResultSiteIds(brokerQueryId)).containsExactly("1");
        assertEquals(2, client.getResultFeasibility(brokerQueryId, "1"));
    }

    @Test
    void testExecuteQueryFemale()
        throws QueryNotFoundException, IOException, SiteNotFoundException, QueryDefinitionNotFoundException {
        var brokerQueryId = client.createQuery(TEST_BACKEND_QUERY_ID);
        var cqlString = FhirHelper.getResourceFileAsString("gender-female.cql");
        client.addQueryDefinition(brokerQueryId, CQL, cqlString);

        var statusListener = mock(QueryStatusListener.class);
        client.addQueryStatusListener(statusListener);
        client.publishQuery(brokerQueryId);

        var statusUpdate = new QueryStatusUpdate(client, brokerQueryId, "1", COMPLETED);
        verify(statusListener, timeout(ASYNC_TIMEOUT_WAIT_MS)).onClientUpdate(TEST_BACKEND_QUERY_ID, statusUpdate);

        assertThat(client.getResultSiteIds(brokerQueryId)).containsExactly("1");
        assertEquals(1, client.getResultFeasibility(brokerQueryId, "1"));
    }

    private void createDummyPatient(IGenericClient client, AdministrativeGender gender, String lastName, String firstName) {
        String patientIdentifier = Integer.toString(ThreadLocalRandom.current().nextInt(99999));
        Patient patient = new Patient();
        patient.addIdentifier()
            .setSystem("http://acme.org/mrns")
            .setValue(patientIdentifier);
        patient.addName()
            .setFamily(lastName)
            .addGiven(firstName);
        patient.setGender(gender);
        patient.setId(IdType.newRandomUuid());

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);


        bundle.addEntry()
            .setFullUrl(patient.getIdElement().getValue())
            .setResource(patient)
            .getRequest()
            .setUrl("Patient")
            .setIfNoneExist("identifier=http://acme.org/mrns|" + patientIdentifier)
            .setMethod(Bundle.HTTPVerb.POST);

        client.transaction().withBundle(bundle).execute();
    }

}
