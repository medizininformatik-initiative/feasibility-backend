package QueryExecutor.impl.dsf;

import QueryExecutor.api.PublishFailedException;
import QueryExecutor.api.QueryNotFoundException;
import QueryExecutor.api.UnsupportedMediaTypeException;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION;
import static org.hl7.fhir.r4.model.Bundle.HTTPVerb.POST;
import static org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE;
import static org.hl7.fhir.r4.model.Task.TaskIntent.ORDER;
import static org.hl7.fhir.r4.model.Task.TaskStatus.REQUESTED;

/**
 * Manager for feasibility queries.
 * <p>
 * Can be used to setup queries and publish them.
 */
class DSFQueryManager implements QueryManager {

    private static final String INSTANTIATE_URI = "http://highmed.org/bpe/Process/requestSimpleFeasibility/0.1.0";
    private static final String PROFILE = "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/codex-task-request-simple-feasibility";
    private static final String REQUEST_URL_TASK = "Task";
    private static final String REQUEST_URL_LIBRARY = "Library";
    private static final String REQUEST_URL_MEASURE = "Measure";
    private static final String MEASURE_RESOURCE_TYPE_REPRESENTATION = "Measure";
    private static final String LIBRARY_RESOURCE_TYPE_REPRESENTATION = "Library";
    private static final String MEASURE_CRITERIA_INITIAL_POPULATION = "InInitialPopulation";
    private static final String BPMN_REQUEST_SIMPLE_FEASIBILITY_MESSAGE = "requestSimpleFeasibilityMessage";

    private static final String CODE_SYSTEM_FEASIBILITY = "https://www.netzwerk-universitaetsmedizin.de/fhir/CodeSystem/feasibility";
    private static final String CODE_SYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE = "measure-reference";
    private static final String CODE_SYSTEM_ORGANIZATION = "http://highmed.org/fhir/NamingSystem/organization-identifier";
    private static final String CODE_SYSTEM_BPMN_MESSAGE = "http://highmed.org/fhir/CodeSystem/bpmn-message";
    private static final String CODE_SYSTEM_BPMN_MESSAGE_VALUE_MESSAGE_NAME = "message-name";
    private static final String CODE_SYSTEM_BPMN_MESSAGE_VALUE_BUSINESS_KEY = "business-key";
    private static final String CODE_SYSTEM_LIBRARY_TYPE = "http://terminology.hl7.org/CodeSystem/library-type";
    private static final String CODE_SYSTEM_LIBRARY_TYPE_VALUE_LOGIC_LIBRARY = "logic-library";
    private static final String CODE_SYSTEM_AUTHORIZATION_ROLE = "http://highmed.org/fhir/CodeSystem/authorization-role";
    private static final String CODE_SYSTEM_AUTHORIZATION_ROLE_VALUE_LOCAL = "LOCAL";
    private static final String CODE_SYSTEM_AUTHORIZATION_ROLE_VALUE_REMOTE = "REMOTE";
    private static final String CODE_SYSTEM_MEASURE_SCORING = "http://terminology.hl7.org/CodeSystem/measure-scoring";
    private static final String CODE_SYSTEM_MEASURE_SCORING_VALUE_COHORT = "cohort";
    private static final String CODE_SYSTEM_MEASURE_POPULATION = "http://terminology.hl7.org/CodeSystem/measure-population";
    private static final String CODE_SYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION = "initial-population";

    private final FhirWebserviceClient fhirWebserviceClient;
    private final String organizationId;
    private final Map<String, Bundle> queryHeap;

    /**
     * Creates a new {@link DSFQueryManager} instance.
     *
     * @param fhirWebserviceClient Client able to communicate with a FHIR server for publishing queries.
     * @param organizationId       Identifies the local FHIR server instance (ZARS) that queries get published to.
     */
    DSFQueryManager(FhirWebserviceClient fhirWebserviceClient, String organizationId) {
        this.fhirWebserviceClient = fhirWebserviceClient;
        this.organizationId = organizationId;
        this.queryHeap = new HashMap<>();
    }

    @Override
    public String createQuery() {
        UUID queryId = UUID.randomUUID();
        queryHeap.put(queryId.toString(), createQueryBundleWithTask(queryId));

        return queryId.toString();
    }

    @Override
    public void addQueryDefinition(String queryId, String mediaType, String content) throws QueryNotFoundException, UnsupportedMediaTypeException {
        Bundle queryBundle = queryHeap.get(queryId);
        if (queryBundle == null) {
            throw new QueryNotFoundException(queryId);
        }
        if (!mediaType.equalsIgnoreCase("text/cql")) {
            throw new UnsupportedMediaTypeException(mediaType, Collections.singletonList("text/cql"));
        }
        if (containsQueryDefinition(queryBundle)) {
            throw new IllegalStateException("Query with ID '" + queryId + "' already contains a query definition.");
        }

        UUID libraryId = UUID.randomUUID();
        Bundle bundleWithLibrary = addLibrary(queryBundle, mediaType, content, libraryId);
        queryHeap.put(queryId, addMeasure(bundleWithLibrary, mediaType, libraryId));
    }

    @Override
    public void publishQuery(String queryId) throws QueryNotFoundException, PublishFailedException {
        Bundle queryBundle = queryHeap.get(queryId);
        if (queryBundle == null) {
            throw new QueryNotFoundException(queryId);
        }
        if (!containsQueryDefinition(queryBundle)) {
            throw new IllegalStateException("Query with ID '" + queryId + "' does not contain query definition yet.");
        }

        try {
            fhirWebserviceClient.postBundle(queryBundle);
        } catch (Exception e) {
            throw new PublishFailedException(queryId, e);
        }
    }

    @Override
    public void removeQuery(String queryId) throws QueryNotFoundException {
        if (!queryHeap.containsKey(queryId)) {
            throw new QueryNotFoundException(queryId);
        }
        queryHeap.remove(queryId);
    }

    /**
     * Creates a canonical url for a {@link UUID} to be used in a FHIR context.
     *
     * @param uuid The UUID to be used.
     * @return The canonical url for the given UUID.
     */
    private String createCanonicalUUIDUrn(UUID uuid) {
        return "urn:uuid:" + uuid;
    }

    /**
     * Creates a query bundle with a task attached to it.
     * <p>
     * The task will make sure that the query gets distributed within the DSF context.
     *
     * @param queryId Identifies the query for later use.
     * @return A {@link Bundle} with a task attached to it.
     */
    private Bundle createQueryBundleWithTask(UUID queryId) {
        Bundle queryBundle = new Bundle().setType(TRANSACTION);

        Task task = new Task()
                .setStatus(REQUESTED)
                .setIntent(ORDER)
                .setAuthoredOn(new Date())
                .setInstantiatesUri(INSTANTIATE_URI);

        task.getRequester()
                .setType("Organization")
                .getIdentifier().setSystem(CODE_SYSTEM_ORGANIZATION).setValue(organizationId);

        task.getRestriction().getRecipientFirstRep()
                .setType("Organization")
                .getIdentifier().setSystem(CODE_SYSTEM_ORGANIZATION).setValue(organizationId);

        task.addInput()
                .setType(new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem(CODE_SYSTEM_BPMN_MESSAGE)
                                .setCode(CODE_SYSTEM_BPMN_MESSAGE_VALUE_MESSAGE_NAME)))
                .setValue(new StringType(BPMN_REQUEST_SIMPLE_FEASIBILITY_MESSAGE));
        task.addInput()
                .setType(new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem(CODE_SYSTEM_BPMN_MESSAGE)
                                .setCode(CODE_SYSTEM_BPMN_MESSAGE_VALUE_BUSINESS_KEY)))
                .setValue(new StringType(queryId.toString()));
        task.setMeta(new Meta().addProfile(PROFILE));

        queryBundle.addEntry()
                .setRequest(new BundleEntryRequestComponent()
                        .setMethod(POST)
                        .setUrl(REQUEST_URL_TASK))
                .setResource(task)
                .setFullUrl(createCanonicalUUIDUrn(UUID.randomUUID()));

        return queryBundle;
    }

    /**
     * Given a query bundle adds a library resource to it.
     * <p>
     * Adds the library with the given content associated with it using the given media type
     * The library
     *
     * @param queryBundle The library is added to this query bundle.
     * @param mediaType   The media type of the library content.
     * @param content     The content of the library.
     * @param libraryId   Identifies the library for referential usage.
     * @return A copy of the given {@link Bundle} with the library added.
     */
    private Bundle addLibrary(Bundle queryBundle, String mediaType, String content, UUID libraryId) {
        Bundle bundle = queryBundle.copy();
        Library library = new Library()
                .setStatus(ACTIVE)
                .setType(new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem(CODE_SYSTEM_LIBRARY_TYPE)
                                .setCode(CODE_SYSTEM_LIBRARY_TYPE_VALUE_LOGIC_LIBRARY)))
                .addContent(new Attachment()
                        .setContentType(mediaType)
                        .setData(content.getBytes(StandardCharsets.UTF_8)))
                .setUrl(createCanonicalUUIDUrn(libraryId));

        library.getMeta()
                .addTag(new Coding()
                        .setSystem(CODE_SYSTEM_AUTHORIZATION_ROLE)
                        .setCode(CODE_SYSTEM_AUTHORIZATION_ROLE_VALUE_LOCAL))
                .addTag(new Coding()
                        .setSystem(CODE_SYSTEM_AUTHORIZATION_ROLE)
                        .setCode(CODE_SYSTEM_AUTHORIZATION_ROLE_VALUE_REMOTE));

        bundle.addEntry()
                .setRequest(new BundleEntryRequestComponent()
                        .setMethod(POST)
                        .setUrl(REQUEST_URL_LIBRARY))
                .setResource(library)
                .setFullUrl(createCanonicalUUIDUrn(UUID.randomUUID()));

        return bundle;
    }

    /**
     * Given a query bundle adds a measure resource to it.
     * <p>
     * Adds a measure resource that references a library using the given library Id.
     * The media type defines the type of the measure criteria.
     *
     * @param queryBundle The measure is added to this query bundle.
     * @param mediaType   The media type of the measure criteria.
     * @param libraryId   Identifies a library that this measure is going to use.
     * @return A copy of the given {@link Bundle} with the measure added.
     */
    private Bundle addMeasure(Bundle queryBundle, String mediaType, UUID libraryId) {
        Bundle bundle = queryBundle.copy();

        UUID measureId = UUID.randomUUID();

        Task task = (Task) bundle.getEntryFirstRep().getResource();
        task.addInput()
                .setType(new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem(CODE_SYSTEM_FEASIBILITY)
                                .setCode(CODE_SYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE)))
                .setValue(new Reference()
                        .setReference(createCanonicalUUIDUrn(measureId)));

        Measure measure = new Measure()
                .setStatus(ACTIVE)
                .setScoring(new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem(CODE_SYSTEM_MEASURE_SCORING)
                                .setCode(CODE_SYSTEM_MEASURE_SCORING_VALUE_COHORT)))
                .addLibrary(createCanonicalUUIDUrn(libraryId));

        measure.addGroup().addPopulation()
                .setCode(new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem(CODE_SYSTEM_MEASURE_POPULATION)
                                .setCode(CODE_SYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION)))
                .setCriteria(new Expression()
                        .setLanguage(mediaType)
                        .setExpression(MEASURE_CRITERIA_INITIAL_POPULATION));

        measure.getMeta()
                .addTag(new Coding()
                        .setSystem(CODE_SYSTEM_AUTHORIZATION_ROLE)
                        .setCode(CODE_SYSTEM_AUTHORIZATION_ROLE_VALUE_LOCAL))
                .addTag(new Coding()
                        .setSystem(CODE_SYSTEM_AUTHORIZATION_ROLE)
                        .setCode(CODE_SYSTEM_AUTHORIZATION_ROLE_VALUE_REMOTE));

        bundle.addEntry()
                .setRequest(new BundleEntryRequestComponent()
                        .setMethod(POST)
                        .setUrl(REQUEST_URL_MEASURE))
                .setResource(measure)
                .setFullUrl(createCanonicalUUIDUrn(measureId));
        return bundle;
    }

    /**
     * Checks whether a given bundle contains a query definition that consists of a measure and a library resource.
     *
     * @param queryBundle The query bundle that shall be checked.
     * @return True if the bundle contains a query definition and false otherwise.
     */
    private boolean containsQueryDefinition(Bundle queryBundle) {
        boolean containsMeasure = false;
        boolean containsLibrary = false;
        for (BundleEntryComponent entry : queryBundle.getEntry()) {
            if (entry.getResource().fhirType().equals(MEASURE_RESOURCE_TYPE_REPRESENTATION)) {
                containsMeasure = true;
            } else if (entry.getResource().fhirType().equals(LIBRARY_RESOURCE_TYPE_REPRESENTATION)) {
                containsLibrary = true;
            }
        }

        return containsMeasure && containsLibrary;
    }
}
