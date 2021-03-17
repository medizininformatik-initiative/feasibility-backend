package de.numcodex.feasibility_gui_backend.service;

import de.numcodex.feasibility_gui_backend.service.query_executor.BrokerClient;
import de.numcodex.feasibility_gui_backend.service.query_executor.impl.mock.MockBrokerClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class BrokerClientProducer {

  public static final String CLIENT_TYPE_DSF = "DSF";
  public static final String CLIENT_TYPE_AKTIN = "AKTIN";
  public static final String CLIENT_TYPE_MOCK = "MOCK";

  private final ApplicationContext ctx;

  public BrokerClientProducer(ApplicationContext ctx) {
      this.ctx = ctx;
  }

  @Qualifier("applied")
  @Bean
  public BrokerClient createBrokerClient(@Value("${app.broker-client}") String type) {
      return switch(StringUtils.upperCase(type)) {
          case CLIENT_TYPE_DSF:
              yield BeanFactoryAnnotationUtils.qualifiedBeanOfType(ctx.getAutowireCapableBeanFactory(), BrokerClient.class, "dsf");
          case CLIENT_TYPE_AKTIN:
              yield BeanFactoryAnnotationUtils.qualifiedBeanOfType(ctx.getAutowireCapableBeanFactory(), BrokerClient.class, "aktin");
          case CLIENT_TYPE_MOCK:
              yield new MockBrokerClient();
          default:
              throw new IllegalStateException(
                      "No Broker Client configured for type '%s'. Allowed types are %s"
                              .formatted(type, List.of(CLIENT_TYPE_DSF, CLIENT_TYPE_AKTIN, CLIENT_TYPE_MOCK)));
      };
  }
}
