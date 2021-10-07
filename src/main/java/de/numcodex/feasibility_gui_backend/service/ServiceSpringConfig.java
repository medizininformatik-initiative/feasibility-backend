package de.numcodex.feasibility_gui_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.repository.QueryRepository;
import de.numcodex.feasibility_gui_backend.repository.ResultRepository;
import de.numcodex.feasibility_gui_backend.repository.SiteRepository;
import de.numcodex.feasibility_gui_backend.service.query_builder.CqlQueryBuilder;
import de.numcodex.feasibility_gui_backend.service.query_builder.FhirQueryBuilder;
import de.numcodex.feasibility_gui_backend.service.query_builder.QueryBuilder;
import de.numcodex.feasibility_gui_backend.service.query_executor.BrokerClient;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatusListener;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatusListenerImpl;
import de.numcodex.feasibility_gui_backend.service.query_executor.impl.mock.MockBrokerClient;
import de.numcodex.sq2cql.Translator;
import de.numcodex.sq2cql.model.ConceptNode;
import de.numcodex.sq2cql.model.Mapping;
import de.numcodex.sq2cql.model.MappingContext;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.MD5;

@Configuration
public class ServiceSpringConfig {

  public static final String CLIENT_TYPE_DSF = "DSF";
  public static final String CLIENT_TYPE_AKTIN = "AKTIN";
  public static final String CLIENT_TYPE_MOCK = "MOCK";
  public static final String CLIENT_TYPE_DIRECT = "DIRECT";

  private final ApplicationContext ctx;

  public ServiceSpringConfig(ApplicationContext ctx) {
    this.ctx = ctx;
  }

  // TODO: use aktin and dsf provider to avoid necessary qualifier annotation
  @Qualifier("applied")
  @Bean
  public BrokerClient createBrokerClient(@Value("${app.brokerClient}") String type) {
    return switch (StringUtils.upperCase(type)) {
      case CLIENT_TYPE_DSF -> BeanFactoryAnnotationUtils
          .qualifiedBeanOfType(ctx.getAutowireCapableBeanFactory(), BrokerClient.class, "dsf");
      case CLIENT_TYPE_AKTIN -> BeanFactoryAnnotationUtils
          .qualifiedBeanOfType(ctx.getAutowireCapableBeanFactory(), BrokerClient.class, "aktin");
      case CLIENT_TYPE_DIRECT -> BeanFactoryAnnotationUtils
          .qualifiedBeanOfType(ctx.getAutowireCapableBeanFactory(), BrokerClient.class, "direct");
      case CLIENT_TYPE_MOCK -> new MockBrokerClient();
      default -> throw new IllegalStateException(
          "No Broker Client configured for type '%s'. Allowed types are %s"
              .formatted(type, List.of(CLIENT_TYPE_DSF, CLIENT_TYPE_AKTIN, CLIENT_TYPE_MOCK)));
    };
  }

  @Bean
  Translator createCqlTranslator(@Value("${app.mappingsFile}") String mappingsFile,
      @Value("${app.conceptTreeFile}") String conceptTreeFile) throws IOException {
    var objectMapper = new ObjectMapper();
    var mappings = objectMapper.readValue(new File(mappingsFile), Mapping[].class);
    var conceptTree = objectMapper.readValue(new File(conceptTreeFile), ConceptNode.class);
    return Translator.of(MappingContext.of(
        Stream.of(mappings)
            .collect(Collectors.toMap(Mapping::getConcept, Function.identity(), (a, b) -> a)),
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
  @Bean
  QueryBuilder createCqlQueryBuilder(Translator translator) {
    return new CqlQueryBuilder(translator, new ObjectMapper());
  }

  @Qualifier("fhir")
  @Bean
  QueryBuilder createFhirQueryBuilder(RestTemplate restTemplate,
      @Value("${app.flare.baseUrl}") String flareBaseUrl) {
    return new FhirQueryBuilder(restTemplate, flareBaseUrl);
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean
  QueryStatusListener createQueryStatusListener(@Qualifier("applied") BrokerClient client,
                                                ResultRepository resultRepository, QueryRepository queryRepository,
                                                SiteRepository siteRepository) {
    return new QueryStatusListenerImpl(resultRepository, queryRepository, siteRepository, client);
  }


  @Qualifier("md5")
  @Bean
  MessageDigest md5MessageDigest() throws NoSuchAlgorithmException {
    return MessageDigest.getInstance(MD5);
  }
}
