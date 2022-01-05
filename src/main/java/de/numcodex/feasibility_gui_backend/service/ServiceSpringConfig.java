package de.numcodex.feasibility_gui_backend.service;

import de.numcodex.feasibility_gui_backend.service.query_executor.BrokerClient;
import de.numcodex.feasibility_gui_backend.service.query_executor.impl.mock.MockBrokerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.codec.digest.MessageDigestAlgorithms.MD5;

@Configuration
@Slf4j
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
      log.info("Enable mock client");
      brokerClients.add(new MockBrokerClient());
    }
    if (directClientEnabled) {
      log.info("Enable direct client");
      brokerClients.add(BeanFactoryAnnotationUtils
          .qualifiedBeanOfType(ctx.getAutowireCapableBeanFactory(), BrokerClient.class, "direct"));
    }
    if (aktinClientEnabled) {
      log.info("Enable aktin client");
      brokerClients.add(BeanFactoryAnnotationUtils
          .qualifiedBeanOfType(ctx.getAutowireCapableBeanFactory(), BrokerClient.class, "aktin"));
    }
    if (dsfClientEnabled) {
      log.info("Enable dsf client");
      brokerClients.add(BeanFactoryAnnotationUtils
          .qualifiedBeanOfType(ctx.getAutowireCapableBeanFactory(), BrokerClient.class, "dsf"));
    }
    return brokerClients;
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Qualifier("md5")
  @Bean
  MessageDigest md5MessageDigest() throws NoSuchAlgorithmException {
    return MessageDigest.getInstance(MD5);
  }
}
