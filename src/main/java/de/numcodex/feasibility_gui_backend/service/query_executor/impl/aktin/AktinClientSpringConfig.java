package de.numcodex.feasibility_gui_backend.service.query_executor.impl.aktin;

import de.numcodex.feasibility_gui_backend.service.query_executor.BrokerClient;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.net.URI;
import java.net.http.WebSocket.Builder;

import org.aktin.broker.client2.AuthFilter;
import org.aktin.broker.client2.BrokerAdmin2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Spring configuration for providing a {@link AktinBrokerClient} instance.
 */
@Configuration
public class AktinClientSpringConfig {

    @Value("${de.num-codex.FeasibilityGuiBackend.aktin.broker.baseUrl}")
    private String brokerBaseUrl;

    @Value("${de.num-codex.FeasibilityGuiBackend.aktin.broker.apiKey}")
    private String brokerApiKey;

    
    @Bean
    public BrokerClient aktinBrokerClient() {
    	BrokerAdmin2 client = new BrokerAdmin2(URI.create(brokerBaseUrl));
    	client.setAuthFilter(new ApiKeyAuthFilter(brokerApiKey));
    	return new AktinBrokerClient(client);
    }

    @AllArgsConstructor
    private static class ApiKeyAuthFilter implements AuthFilter{
    	private String key;
		@Override
		public void addAuthentication(Builder builder) throws IOException {
			builder.header("Authorization", "Bearer "+key);
		}

		@Override
		public void addAuthentication(java.net.http.HttpRequest.Builder builder) throws IOException {
			builder.header("Authorization", "Bearer "+key);
		}
    }

}
