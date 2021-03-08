package de.numcodex.feasibility_gui_backend.service.query_executor.impl.dsf;

import ca.uhn.fhir.context.FhirContext;
import de.numcodex.feasibility_gui_backend.service.query_executor.BrokerClient;
import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.CertificateReader;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.highmed.dsf.fhir.service.ReferenceCleanerImpl;
import org.highmed.dsf.fhir.service.ReferenceExtractorImpl;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.highmed.fhir.client.FhirWebserviceClientJersey;
import org.highmed.fhir.client.WebsocketClient;
import org.highmed.fhir.client.WebsocketClientTyrus;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Subscription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType.WEBSOCKET;
import static org.hl7.fhir.r4.model.Subscription.SubscriptionStatus.ACTIVE;
import static org.hl7.fhir.r4.model.Task.TaskStatus.COMPLETED;

/**
 * Spring configuration for providing a {@link DSFBrokerClient} instance.
 */
@Configuration
public class DSFSpringConfig {

    private static final String QUERY_RESULT_SUBSCRIPTION_REASON = "Waiting for query results";
    private static final String QUERY_RESULT_SUBSCRIPTION_CHANNEL_PAYLOAD = "application/fhir+json";
    //    private static final String SINGLE_DIC_QUERY_RESULT_PROFILE = "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/codex-task-single-dic-result-simple-feasibility";

    @Data
    @AllArgsConstructor
    private static class SecurityContext {
        KeyStore keyStore;
        KeyStore trustStore;
    }

    @Value("${de.num-codex.FeasibilityGuiBackend.dsf.security.keystore.p12file}")
    private String keyStoreFile;

    @Value("${de.num-codex.FeasibilityGuiBackend.dsf.security.keystore.password}")
    private char[] keyStorePassword;

    @Value("${de.num-codex.FeasibilityGuiBackend.dsf.security.certificate}")
    private String certificateFile;

    @Value("${de.num-codex.FeasibilityGuiBackend.dsf.webservice.baseUrl}")
    private String webserviceBaseUrl;

    @Value("${de.num-codex.FeasibilityGuiBackend.dsf.webservice.readTimeout}")
    private int webserviceReadTimeout;

    @Value("${de.num-codex.FeasibilityGuiBackend.dsf.webservice.connectTimeout}")
    private int webserviceConnectTimeout;

    @Value("${de.num-codex.FeasibilityGuiBackend.dsf.websocket.url}")
    private String websocketUrl;

    @Value("${de.num-codex.FeasibilityGuiBackend.dsf.organizationId}")
    private String organizationId;

    @Bean
    public BrokerClient dsfBrokerClient(QueryManager queryManager, QueryResultCollector queryResultCollector) {
        return new DSFBrokerClient(queryManager, queryResultCollector);
    }

    @Bean
    QueryManager dsfQueryManager(FhirWebserviceClient client) {
        return new DSFQueryManager(client, organizationId.replace(' ', '_'));
    }

    @Bean
    QueryResultCollector queryResultCollector(QueryResultStore resultStore, FhirContext fhirContext,
                                              WebsocketClient websocketClient, DSFQueryResultHandler resultHandler) {
        return new DSFQueryResultCollector(resultStore, fhirContext, websocketClient, resultHandler);
    }

    @Bean
    QueryResultStore queryResultStore() {
        return new DSFQueryResultStore();
    }

    @Bean
    DSFQueryResultHandler queryResultHandler(FhirWebserviceClient webserviceClient) {
        return new DSFQueryResultHandler(webserviceClient);
    }

    @Bean
    FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    @Bean
    SecurityContext securityContext() throws IOException, CertificateException, NoSuchAlgorithmException,
            KeyStoreException {
        Path localWebsocketKsFile = Paths.get(keyStoreFile);
        if (!Files.isReadable(localWebsocketKsFile)) {
            throw new IOException("Keystore file '" + localWebsocketKsFile.toString() + "' not readable");
        }
        KeyStore localKeyStore = CertificateReader.fromPkcs12(localWebsocketKsFile, keyStorePassword);
        KeyStore localTrustStore = CertificateHelper.extractTrust(localKeyStore);

        if (!Files.isReadable(Paths.get(certificateFile))) {
            throw new IOException("Certificate file '" + certificateFile + "' not readable");
        }
        FileInputStream inStream = new FileInputStream(certificateFile);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate cert = cf.generateCertificate(inStream);

        localTrustStore.setCertificateEntry("zars", cert);

        return new SecurityContext(localKeyStore, localTrustStore);
    }

    @Bean
    FhirWebserviceClient fhirWebserviceClient(SecurityContext securityCtx) {

        ReferenceExtractorImpl extractor = new ReferenceExtractorImpl();
        ReferenceCleanerImpl cleaner = new ReferenceCleanerImpl(extractor);

        return new FhirWebserviceClientJersey(
                webserviceBaseUrl,
                securityCtx.trustStore,
                securityCtx.keyStore,
                keyStorePassword,
                null,
                null,
                null,
                webserviceConnectTimeout,
                webserviceReadTimeout,
                null,
                fhirContext(),
                cleaner);
    }

    @Bean
    WebsocketClient fhirWebsocketClient(FhirWebserviceClient fhirClient, SecurityContext securityCtx) {
        // TODO: change to provider
        String subscriptionId = searchForExistingQueryResultSubscription(fhirClient)
                .orElseGet(createQueryResultSubscription(fhirClient))
                .getIdElement().getIdPart();

        // TODO: implement reconnector
        return new WebsocketClientTyrus(() -> {
        },
                URI.create(websocketUrl),
                securityCtx.trustStore,
                securityCtx.keyStore,
                keyStorePassword,
                subscriptionId);
    }

    /**
     * Searches for an existing feasibility query result subscription and returns it if there is any.
     *
     * @return The subscription for query results.
     */
    private Optional<Subscription> searchForExistingQueryResultSubscription(FhirWebserviceClient fhirClient) {
        Bundle bundle = fhirClient.searchWithStrictHandling(Subscription.class,
                Map.of("criteria", Collections.singletonList("Task?status=" + COMPLETED.toCode()),
                        "status", Collections.singletonList(ACTIVE.toCode()),
                        "type", Collections.singletonList(WEBSOCKET.toCode()),
                        "payload", Collections.singletonList(QUERY_RESULT_SUBSCRIPTION_CHANNEL_PAYLOAD)));

        if (!Bundle.BundleType.SEARCHSET.equals(bundle.getType()))
            throw new RuntimeException("Could not retrieve searchset for subscription search query, but got "
                    + bundle.getType());
        if (bundle.getTotal() == 0)
            return Optional.empty();
        if (bundle.getTotal() != 1)
            throw new RuntimeException("Could not retrieve exactly one result for subscription search query");
        if (!(bundle.getEntryFirstRep().getResource() instanceof Subscription))
            throw new RuntimeException("Could not retrieve exactly one Subscription, but got "
                    + bundle.getEntryFirstRep().getResource().getResourceType());

        return Optional.of((Subscription) bundle.getEntryFirstRep().getResource());
    }

    /**
     * Returns a function capable of supplying a newly created subscription for feasibility query results.
     *
     * @return Function for getting a subscription for feasibility query results.
     */
    private Supplier<Subscription> createQueryResultSubscription(FhirWebserviceClient fhirClient) {
        return () -> {
            Subscription subscription = new Subscription()
                    .setStatus(ACTIVE)
                    .setReason(QUERY_RESULT_SUBSCRIPTION_REASON)
                    .setChannel(new Subscription.SubscriptionChannelComponent()
                            .setType(WEBSOCKET)
                            .setPayload(QUERY_RESULT_SUBSCRIPTION_CHANNEL_PAYLOAD))
                    // TODO: use this criteria if DSF has implemented the _profile search parameter (make sure to also remove the profile check in the DSFQueryResultHandler class!)
//                .setCriteria("Task?status=" + Task.TaskStatus.COMPLETED.toCode() + "&_profile=" + SINGLE_DIC_QUERY_RESULT_PROFILE);
                    .setCriteria("Task?status=" + COMPLETED.toCode());

            subscription.getMeta()
                    .addTag()
                    .setSystem("http://highmed.org/fhir/CodeSystem/authorization-role")
                    .setCode("LOCAL");

            return fhirClient.create(subscription);
        };
    }
}
