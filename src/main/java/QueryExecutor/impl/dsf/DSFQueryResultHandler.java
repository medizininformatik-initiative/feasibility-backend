package QueryExecutor.impl.dsf;

import lombok.extern.slf4j.Slf4j;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;

import java.util.Optional;

/**
 * A handler that is capable of processing a FHIR Task which represents the result of a feasibility query that ran
 * in a single DIC.
 */
@Slf4j
class DSFQueryResultHandler {

    private static final String SINGLE_DIC_QUERY_RESULT_PROFILE = "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/codex-task-single-dic-result-simple-feasibility";
    private static final String CODE_SYSTEM_FEASIBILITY = "https://www.netzwerk-universitaetsmedizin.de/fhir/CodeSystem/feasibility";
    private static final String CODE_SYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REF = "measure-report-reference";
    private static final String CODE_SYSTEM_BPMN_MESSAGE = "http://highmed.org/fhir/CodeSystem/bpmn-message";
    private static final String CODE_SYSTEM_BPMN_MESSAGE_VALUE_BUSINESS_KEY = "business-key";

    private static final String CODE_SYSTEM_MEASURE_POPULATION = "http://terminology.hl7.org/CodeSystem/measure-population";
    private static final String CODE_SYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION = "initial-population";

    private final FhirWebserviceClient client;

    /**
     * Creates a new {@link DSFQueryResultHandler} instance for processing FHIR Task resources.
     *
     * @param client FHIR webservice client for resolving references within a FHIR Task.
     */
    public DSFQueryResultHandler(FhirWebserviceClient client) {
        this.client = client;
    }

    /**
     * Given a {@link DomainResource} creates a {@link DSFQueryResult}.
     * <p>
     * Only {@link DomainResource}s that resemble a {@link Task} will be taken into account. Such a task itself has to
     * have a profile indicating that it carries information regarding a feasibility query result of a single DIC.
     *
     * @param resource An arbitrary FHIR resource.
     * @return The feasibility query result if there is any.
     */
    public Optional<DSFQueryResult> onResult(DomainResource resource) {
        if (resource instanceof Task) {
            Task task = (Task) resource;

            if (!hasSingleQueryResultProfile(task)) {
                log.info("Ignoring task without single DIC query result profile.");
                return Optional.empty();
            }

            String queryId = extractQueryId(task);
            String clientId = task.getRequester().getIdentifier().getValue();
            IdType measureReportUrl = extractMeasureReportId(task);

            log.info("Received query result of query with ID '" + queryId + "' for client with ID '" + clientId + "'");

            try {
                MeasureReport report = fetchMeasureReport(measureReportUrl.getIdPart());
                int measureCount = extractMeasureCount(report);
                return Optional.of(new DSFQueryResult(queryId, clientId, measureCount));
            } catch (Exception e) {
                log.error("Could not fetch measure report with ID '" + measureReportUrl.getIdPart()
                        + "' to get the measure count of query with ID '" + queryId + "' for client with ID '"
                        + clientId + "': " + e.getMessage(), e);
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     * Checks whether the given task has a single query result profile attached to it.
     *
     * @param task The task that shall be checked for the profile.
     * @return True if the task has a single query result profile attached to it and false otherwise.
     */
    private boolean hasSingleQueryResultProfile(Task task) {
        return task.getMeta().getProfile().stream().anyMatch(p -> p.getValue().equals(SINGLE_DIC_QUERY_RESULT_PROFILE));
    }

    /**
     * Given a FHIR Task extracts the query ID from it.
     *
     * @param task The task that the query ID is extracted from.
     * @return The query ID.
     */
    private String extractQueryId(Task task) {
        return task.getInput()
                .stream()
                .filter(i -> i.getType().getCodingFirstRep().getSystem().equals(CODE_SYSTEM_BPMN_MESSAGE)
                        && i.getType().getCodingFirstRep().getCode().equals(CODE_SYSTEM_BPMN_MESSAGE_VALUE_BUSINESS_KEY))
                .findFirst().orElseThrow().getValue().toString();
    }

    /**
     * Given a FHIR Task extracts the URL pointing to the measure report holding any measure count information.
     *
     * @param task The task that the measure report URL is extracted from.
     * @return The measure report URL.
     */
    private IdType extractMeasureReportId(Task task) {
        Reference measureReportReference = (Reference) task.getOutput()
                .stream()
                .filter(o -> o.getType().getCodingFirstRep().getSystem().equals(CODE_SYSTEM_FEASIBILITY)
                        && o.getType().getCodingFirstRep().getCode().equals(CODE_SYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REF))
                .findFirst().orElseThrow().getValue();

        return new IdType(measureReportReference.getReference());
    }

    /**
     * Fetches a specific measure report and returns it.
     *
     * @param measureReportId Identifies the measure report that shall be fetched.
     * @return The fetched measure report.
     */
    private MeasureReport fetchMeasureReport(String measureReportId) {
        return client.read(MeasureReport.class, measureReportId);
    }

    /**
     * Given a {@link MeasureReport} extracts the measure count from it.
     *
     * @param report The report from which the measure count shall be extracted.
     * @return The extracted measure count.
     */
    private int extractMeasureCount(MeasureReport report) {
        return report.getGroup()
                .stream()
                .filter(g -> g.getPopulationFirstRep().getCode().getCodingFirstRep().getSystem().equals(CODE_SYSTEM_MEASURE_POPULATION)
                        && g.getPopulationFirstRep().getCode().getCodingFirstRep().getCode().equals(CODE_SYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION))
                .findFirst().orElseThrow().getPopulationFirstRep().getCount();
    }
}
