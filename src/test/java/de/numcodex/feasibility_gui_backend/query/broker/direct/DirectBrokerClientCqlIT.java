package de.numcodex.feasibility_gui_backend.query.broker.direct;

import static de.numcodex.feasibility_gui_backend.query.QueryMediaType.CQL;
import static de.numcodex.feasibility_gui_backend.query.collect.QueryStatus.COMPLETED;
import static java.lang.String.format;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.numcodex.feasibility_gui_backend.query.broker.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.query.broker.SiteNotFoundException;
import de.numcodex.feasibility_gui_backend.query.collect.QueryStatusListener;
import de.numcodex.feasibility_gui_backend.query.collect.QueryStatusUpdate;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
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

    private final Map<String, String> CODE_SYSTEM_ALIASES = Map.ofEntries(
        entry("http://fhir.de/CodeSystem/dimdi/icd-10-gm", "icd10"),
        entry("http://loinc.org", "loinc"),
        entry("https://fhir.bbmri.de/CodeSystem/SampleMaterialType", "sample"),
        entry("http://fhir.de/CodeSystem/dimdi/atc", "atc"),
        entry("http://snomed.info/sct", "snomed"),
        entry("http://terminology.hl7.org/CodeSystem/condition-ver-status", "cvs"),
        entry("http://hl7.org/fhir/administrative-gender", "gender"),
        entry(
            "https://www.netzwerk-universitaetsmedizin.de/fhir/CodeSystem/ecrf-parameter-codes",
            "num-ecrf"),
        entry("urn:iso:std:iso:3166", "iso3166"),
        entry("https://www.netzwerk-universitaetsmedizin.de/fhir/CodeSystem/frailty-score",
            "fraility-score"),
        entry("http://terminology.hl7.org/CodeSystem/consentcategorycodes", "consent"),
        entry("urn:oid:2.16.840.1.113883.3.1937.777.24.5.1", "mide-1"),
        entry("http://hl7.org/fhir/consent-provision-type", "provision-type"));


    private final GenericContainer<?> blaze = new GenericContainer<>(
        DockerImageName.parse("samply/blaze:0.18"))
        .withImagePullPolicy(PullPolicy.alwaysPull())
        .withExposedPorts(8080)
        .waitingFor(Wait.forHttp("/health").forStatusCodeMatching(c -> c >= 200 && c <= 500))
        .withStartupAttempts(3);

    DirectBrokerClientCql client;
    private final FhirContext fhirContext = FhirContext.forR4();
    private IGenericClient fhirClient;

    @BeforeEach
    void setUp() {
        blaze.start();
        fhirContext.getRestfulClientFactory().setSocketTimeout(200 * 1000);
        fhirClient = fhirContext.newRestfulGenericClient(
            format("http://localhost:%d/fhir", blaze.getFirstMappedPort()));
        client = new DirectBrokerClientCql(fhirContext, fhirClient);
    }

    @AfterEach
    void tearDown() {
        blaze.stop();
    }

    @Test
    void testExecuteQuery() throws QueryNotFoundException, IOException, InterruptedException, SiteNotFoundException {
        var brokerQueryId = client.createQuery(TEST_BACKEND_QUERY_ID);
        var cqlString = DirectBrokerClientCql.getResourceFileAsString("cql/gender-male.cql");
        client.addQueryDefinition(brokerQueryId, CQL.getRepresentation(), cqlString);

        var statusListener = mock(QueryStatusListener.class);
        client.addQueryStatusListener(statusListener);
        client.publishQuery(brokerQueryId);

        var statusUpdate = new QueryStatusUpdate(client, brokerQueryId, "1", COMPLETED);
        verify(statusListener, timeout(ASYNC_TIMEOUT_WAIT_MS)).onClientUpdate(TEST_BACKEND_QUERY_ID, statusUpdate);

        assertEquals(1, client.getResultSiteIds(brokerQueryId).size());
        assertEquals("1", client.getResultSiteIds(brokerQueryId).get(0));
        assertEquals(0, client.getResultFeasibility(brokerQueryId, "1"));
    }

}
