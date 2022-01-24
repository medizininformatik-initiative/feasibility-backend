package de.numcodex.feasibility_gui_backend.query.broker.dsf;

import ca.uhn.fhir.context.FhirContext;
import de.numcodex.feasibility_gui_backend.query.broker.BrokerClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;


/**
 * Spring configuration for providing a {@link DSFBrokerClient} instance.
 */
@Lazy
@Configuration
public class DSFSpringConfig {

    @Value("${app.broker.dsf.security.keystore.p12file}")
    private String keyStoreFile;

    @Value("${app.broker.dsf.security.keystore.password}")
    private char[] keyStorePassword;

    @Value("${app.broker.dsf.security.certificate}")
    private String certificateFile;

    @Value("${app.broker.dsf.proxy.host}")
    private String proxyHost;

    @Value("${app.broker.dsf.proxy.username}")
    private String proxyUsername;

    @Value("${app.broker.dsf.proxy.password}")
    private String proxyPassword;

    @Value("${app.broker.dsf.webservice.baseUrl}")
    private String webserviceBaseUrl;

    @Value("${app.broker.dsf.webservice.readTimeout}")
    private int webserviceReadTimeout;

    @Value("${app.broker.dsf.webservice.connectTimeout}")
    private int webserviceConnectTimeout;

    @Value("${app.broker.dsf.websocket.url}")
    private String websocketUrl;

    @Value("${app.broker.dsf.organizationId}")
    private String organizationId;

    @Qualifier("dsf")
    @Bean
    public BrokerClient dsfBrokerClient(QueryManager queryManager, QueryResultCollector queryResultCollector) {
        return new DSFBrokerClient(queryManager, queryResultCollector);
    }

    @Bean
    QueryManager dsfQueryManager(FhirWebClientProvider fhirWebClientProvider, DSFMediaTypeTranslator dsfMediaTypeTranslator) {
        return new DSFQueryManager(fhirWebClientProvider, dsfMediaTypeTranslator,
                organizationId.replace(' ', '_'));
    }

    @Bean
    DSFMediaTypeTranslator dsfMediaTypeTranslator() {
        return new DSFMediaTypeTranslator();
    }

    @Bean
    QueryResultCollector queryResultCollector(QueryResultStore resultStore, FhirContext fhirContext,
                                              FhirWebClientProvider webClientProvider, DSFQueryResultHandler resultHandler) {
        return new DSFQueryResultCollector(resultStore, fhirContext, webClientProvider, resultHandler);
    }

    @Bean
    QueryResultStore queryResultStore() {
        return new DSFQueryResultStore();
    }

    @Bean
    DSFQueryResultHandler queryResultHandler(FhirWebClientProvider webClientProvider) {
        return new DSFQueryResultHandler(webClientProvider);
    }

    @Bean
    FhirContext fhirContext() {
        return FhirContext.forR4();
    }


    @Bean
    FhirSecurityContextProvider fhirSecurityContextProvider() {
        return new DSFFhirSecurityContextProvider(keyStoreFile, keyStorePassword, certificateFile);
    }

    @Bean
    FhirProxyContext fhirProxyContext() {
        return new FhirProxyContext(
                (proxyHost.isEmpty()) ? null : proxyHost,
                (proxyUsername.isEmpty()) ? null : proxyUsername,
                (proxyPassword.isEmpty()) ? null : proxyPassword
        );
    }

    @Bean
    FhirWebClientProvider fhirWebClientProvider(FhirContext fhirContext,
                                                FhirSecurityContextProvider securityContextProvider,
                                                FhirProxyContext proxyContext) {
        return new DSFFhirWebClientProvider(fhirContext, webserviceBaseUrl, webserviceReadTimeout,
                webserviceConnectTimeout, websocketUrl, securityContextProvider, proxyContext);
    }

}
