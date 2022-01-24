package de.numcodex.feasibility_gui_backend.query.translation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.QueryMediaType;
import de.numcodex.sq2cql.Translator;
import de.numcodex.sq2cql.model.Mapping;
import de.numcodex.sq2cql.model.MappingContext;
import de.numcodex.sq2cql.model.TermCodeNode;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.numcodex.feasibility_gui_backend.query.QueryMediaType.*;
import static java.util.Map.entry;

@Configuration
public class QueryTranslatorSpringConfig {

    @Value("${app.mappingsFile}")
    private String mappingsFile;

    @Value("${app.conceptTreeFile}")
    private String conceptTreeFile;

    @Value("${app.flare.baseUrl}")
    private String flareBaseUrl;

    @Value("${app.cqlTranslationEnabled}")
    private boolean cqlTranslationEnabled;

    @Value("${app.fhirTranslationEnabled}")
    private boolean fhirTranslationEnabled;

    private final ApplicationContext appContext;

    public QueryTranslatorSpringConfig(ApplicationContext appContext) {
        this.appContext = Objects.requireNonNull(appContext);
    }

    private QueryTranslator getQueryTranslatorByQualifier(String qualifier) {
        return BeanFactoryAnnotationUtils
                .qualifiedBeanOfType(appContext.getAutowireCapableBeanFactory(), QueryTranslator.class, qualifier);
    }

    @Bean
    QueryTranslationComponent createQueryTranslationService(@Qualifier("json") QueryTranslator jsonQueryTranslator) {
        var queryTranslators = new HashMap<QueryMediaType, QueryTranslator>();
        queryTranslators.put(STRUCTURED_QUERY, jsonQueryTranslator);

        if (fhirTranslationEnabled) {
            queryTranslators.put(FHIR, getQueryTranslatorByQualifier("fhir"));
        }

        if (cqlTranslationEnabled) {
            queryTranslators.put(CQL, getQueryTranslatorByQualifier("cql"));
        }
        return new QueryTranslationComponent(queryTranslators);
    }

    @Lazy
    @Bean
    Translator createCqlTranslator(@Qualifier("translation") ObjectMapper jsonUtil) throws IOException {
        var mappings = jsonUtil.readValue(new File(mappingsFile), Mapping[].class);
        var conceptTree = jsonUtil.readValue(new File(conceptTreeFile), TermCodeNode.class);
        return Translator.of(MappingContext.of(
                Stream.of(mappings)
                        .collect(Collectors.toMap(Mapping::getKey, Function.identity(), (a, b) -> a)),
                conceptTree,
                Map.ofEntries(entry("http://fhir.de/CodeSystem/dimdi/icd-10-gm", "icd10"),
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
                        entry("http://hl7.org/fhir/consent-provision-type", "provision-type"))));
    }

    @Qualifier("cql")
    @Lazy
    @Bean
    QueryTranslator createCqlQueryTranslator(
            Translator sq2cqlTranslator,
            @Qualifier("translation") ObjectMapper jsonUtil) {
        return new CqlQueryTranslator(sq2cqlTranslator, jsonUtil);
    }

    @Qualifier("fhir")
    @Lazy
    @Bean
    QueryTranslator createFhirQueryBuilder(
            @Qualifier("flare") RestTemplate flareWebClient,
            @Qualifier("translation") ObjectMapper jsonUtil) {
        return new FhirQueryTranslator(flareWebClient, jsonUtil);
    }

    @Qualifier("json")
    @Bean
    QueryTranslator createJsonQueryTranslator(@Qualifier("translation") ObjectMapper jsonUtil) {
        return new JsonQueryTranslator(jsonUtil);
    }

    @Qualifier("flare")
    @Bean
    RestTemplate createFlareWebClient() {
        return new RestTemplateBuilder()
                .rootUri(flareBaseUrl)
                .build();
    }

    @Qualifier("translation")
    @Bean
    ObjectMapper createTranslationObjectMapper() {
        return new ObjectMapper();
    }
}
