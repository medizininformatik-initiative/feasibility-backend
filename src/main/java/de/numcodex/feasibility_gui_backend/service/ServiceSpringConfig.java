package de.numcodex.feasibility_gui_backend.service;

import static java.util.Map.entry;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.MD5;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.ThrowingConsumer;
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
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ServiceSpringConfig {

  @Value("${app.broker.mock.enabled}")
  private boolean mockClientEnabled;

  @Value("${app.broker.direct.enabled}")
  private boolean directClientEnabled;

  @Value("${app.broker.aktin.enabled}")
  private boolean aktinClientEnabled;

  @Value("${app.broker.dsf.enabled}")
  private boolean dsfClientEnabled;

  private final ApplicationContext ctx;

  public ServiceSpringConfig(ApplicationContext ctx) {
    this.ctx = ctx;
  }

  // Do NOT remove the qualifier annotation, since spring attempts to initialize ALL broker clients
  // and does not call this method anymore - rendering the enable-switches moot.
  @Qualifier("applied")
  @Bean
  public List<BrokerClient> createBrokerClients() {
    List<BrokerClient> brokerClients = new ArrayList<>();
    if (mockClientEnabled) {
      brokerClients.add(new MockBrokerClient());
    }
    if (directClientEnabled) {
      brokerClients.add(BeanFactoryAnnotationUtils
          .qualifiedBeanOfType(ctx.getAutowireCapableBeanFactory(), BrokerClient.class, "direct"));
    }
    if (aktinClientEnabled) {
      brokerClients.add(BeanFactoryAnnotationUtils
          .qualifiedBeanOfType(ctx.getAutowireCapableBeanFactory(), BrokerClient.class, "aktin"));
    }
    if (dsfClientEnabled) {
      brokerClients.add(BeanFactoryAnnotationUtils
          .qualifiedBeanOfType(ctx.getAutowireCapableBeanFactory(), BrokerClient.class, "dsf"));
    }
    return brokerClients;
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
  List<QueryStatusListener> createQueryStatusListener(@Qualifier("applied") List<BrokerClient> clients,
                                                ResultRepository resultRepository, QueryRepository queryRepository,
                                                SiteRepository siteRepository) throws IOException {
    var queryStatusListeners = new ArrayList<QueryStatusListener>();
    clients.forEach(throwingConsumerWrapper(client -> {
          QueryStatusListener queryStatusListener = new QueryStatusListenerImpl(resultRepository,
              queryRepository, siteRepository, client);
          queryStatusListeners.add(queryStatusListener);
          client.addQueryStatusListener(queryStatusListener);
        })
    );
    return queryStatusListeners;
  }

  @Qualifier("md5")
  @Bean
  MessageDigest md5MessageDigest() throws NoSuchAlgorithmException {
    return MessageDigest.getInstance(MD5);
  }

  static <T> Consumer<T> throwingConsumerWrapper(
      ThrowingConsumer<T, Exception> throwingConsumer) {

    return i -> {
      try {
        throwingConsumer.accept(i);
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    };
  }
}
