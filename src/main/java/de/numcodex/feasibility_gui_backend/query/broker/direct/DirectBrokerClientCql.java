package de.numcodex.feasibility_gui_backend.query.broker.direct;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.StringParam;
import de.numcodex.feasibility_gui_backend.query.broker.BrokerClient;
import de.numcodex.feasibility_gui_backend.query.collect.QueryStatus;
import de.numcodex.feasibility_gui_backend.query.broker.QueryNotFoundException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;

import java.io.IOException;
import java.util.*;

import static de.numcodex.feasibility_gui_backend.query.QueryMediaType.CQL;
import static de.numcodex.feasibility_gui_backend.query.collect.QueryStatus.COMPLETED;
import static de.numcodex.feasibility_gui_backend.query.collect.QueryStatus.FAILED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION;
import static org.hl7.fhir.r4.model.Bundle.HTTPVerb.POST;

/**
 * A {@link BrokerClient} to be used to directly communicate with a CQL-capable FHIR Server instance
 * without the need for using any middleware (Aktin or DSF).
 */
@Slf4j
public class DirectBrokerClientCql extends DirectBrokerClient {

    private static final String SITE_1_NAME = "CQL Server";
    private final FhirContext fhirContext;
    private final IGenericClient fhirClient;

    /**
     * Creates a new {@link DirectBrokerClientCql} instance that uses the given web client to
     * communicate with a CQL capable FHIR server instance.
     *
     * @param fhirContext A FHIR context.
     * @param fhirClient A FHIR client, configured with the correct url
     */
    public DirectBrokerClientCql(FhirContext fhirContext, IGenericClient fhirClient) {
        this.fhirContext = Objects.requireNonNull(fhirContext);
        this.fhirClient = Objects.requireNonNull(fhirClient);
        listeners = new ArrayList<>();
        brokerQueries = new HashMap<>();
        brokerToBackendQueryIdMapping = new HashMap<>();
    }

    @Override
    public void publishQuery(String brokerQueryId) throws QueryNotFoundException, IOException {
        var query = findQuery(brokerQueryId);
        var queryContent = Optional.ofNullable(query.getQueryDefinition(CQL))
            .orElseThrow(() -> new IllegalStateException("Query with ID "
                + query.getQueryId()
                + " does not contain a query definition for the mandatory type: "
                + CQL
            ));
        try {
            updateQueryStatus(query, QueryStatus.EXECUTING);
            var measureUri = createMeasureAndLibrary(queryContent);
            var report = evaluateMeasure(measureUri);
            var resultCount = report.getGroupFirstRep().getPopulationFirstRep().getCount();
            query.registerSiteResults(SITE_1_ID, obfuscateResultCount ? obfuscate(resultCount) : resultCount);
            updateQueryStatus(query, COMPLETED);
        } catch (Exception e) {
            updateQueryStatus(query, FAILED);
            throw new IOException("An error occurred while publishing the query with ID: " + query.getQueryId(), e);
        }
    }

    @Override
    public String getSiteName(String siteId) {
        return siteId.equals(SITE_1_ID) ? SITE_1_NAME : "";
    }

    /**
     * Create FHIR {@link Measure} and {@link Library} Resources and transmit them in a bundled transaction.
     * @param cql the plaintext cql definition
     * @return the randomly generated identifier of the {@link Measure} resource
     */
    private String createMeasureAndLibrary(String cql) {
        var libraryUri = "urn:uuid" + UUID.randomUUID();
        var library = appendCql(parseResource(Library.class,
            getResourceFileAsString("query/cql/Library.json")).setUrl(libraryUri), cql);
        var measureUri = "urn:uuid" + UUID.randomUUID();
        var measure = parseResource(Measure.class,
            getResourceFileAsString("query/cql/Measure.json"))
            .setUrl(measureUri)
            .addLibrary(libraryUri);
        var bundle = createBundle(library, measure);

        fhirClient.transaction().withBundle(bundle).execute();

        return measureUri;
    }

    /**
     * Get the {@link MeasureReport} for a previously transmitted {@link Measure}
     * @param measureUri the identifier of the {@link Measure}
     * @return the retrieved {@link MeasureReport} from the server
     */
    private MeasureReport evaluateMeasure(String measureUri) {
        return fhirClient.operation()
            .onType(Measure.class)
            .named("evaluate-measure")
            .withSearchParameter(Parameters.class, "measure", new StringParam(measureUri))
            .andSearchParameter("periodStart", new DateParam("1900"))
            .andSearchParameter("periodEnd", new DateParam("2100"))
            .useHttpGet()
            .returnResourceType(MeasureReport.class)
            .execute();
    }

    /**
     * Read file contents as String
     * @param fileName name of the resource file
     * @return the String contents of the file
     */
    public static String getResourceFileAsString(String fileName) {
        InputStream is = getResourceFileAsInputStream(fileName);
        if (is != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } else {
            throw new RuntimeException("resource not found");
        }
    }

    /**
     * Read file contents as {@link InputStream}
     * @param fileName name of the resource file
     * @return an {@link InputStream} of the file
     */
    public static InputStream getResourceFileAsInputStream(String fileName) {
        ClassLoader classLoader = DirectBrokerClientCql.class.getClassLoader();
        return classLoader.getResourceAsStream(fileName);
    }

    /**
     * Parse a String as an {@link IBaseResource} implementation
     * @param type the concrete {@link IBaseResource} implementation class to parse to
     * @param input the {@link String} to parse
     * @return the wanted {@link IBaseResource} implementation object
     * @param <T> any implementation of {@link IBaseResource}
     */
    private <T extends IBaseResource> T parseResource(Class<T> type, String input) {
        var parser = fhirContext.newJsonParser();
        return type.cast(parser.parseResource(input));
    }

    /**
     * Add the CQL query to a {@link Library}
     * @param library the {@link Library} to add the CQL string to
     * @param cql the CQL string to add
     * @return the {@link Library} with the added CQL
     */
    private Library appendCql(Library library, String cql) {
        library.getContentFirstRep().setContentType(CQL.getRepresentation());
        library.getContentFirstRep().setData(cql.getBytes(UTF_8));
        return library;
    }

    /**
     * Create a {@link Bundle} of a {@link Library} and a {@link Measure}
     * @param library the {@link Library} to add to the {@link Bundle}
     * @param measure the {@link Measure} to add to the {@link Bundle}
     * @return the {@link Bundle}, consisting of the given {@link Library} and {@link Measure}
     */
    private static Bundle createBundle(Library library, Measure measure) {
        var bundle = new Bundle();
        bundle.setType(TRANSACTION);
        bundle.addEntry().setResource(library).getRequest().setMethod(POST).setUrl("Library");
        bundle.addEntry().setResource(measure).getRequest().setMethod(POST).setUrl("Measure");
        return bundle;
    }

}
