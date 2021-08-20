package de.numcodex.feasibility_gui_backend.service.query_executor.impl.direct;

import de.numcodex.feasibility_gui_backend.service.query_executor.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatusListener;
import de.numcodex.feasibility_gui_backend.service.query_executor.SiteNotFoundException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static de.numcodex.feasibility_gui_backend.service.QueryMediaTypes.STRUCTURED_QUERY;
import static de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatus.COMPLETED;
import static de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatus.FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ExtendWith(MockitoExtension.class)
class DirectBrokerClientIT {

    private static final int ASYNC_TIMEOUT_WAIT_MS = 2000;

    DirectBrokerClient client;
    WebClient webClient;
    MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        webClient = WebClient.create(mockWebServer.url("/").toString());
        client = new DirectBrokerClient(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testPublishQuery() throws QueryNotFoundException, IOException, InterruptedException, SiteNotFoundException {
        var queryId = client.createQuery();
        client.addQueryDefinition(queryId, STRUCTURED_QUERY, "foo");

        mockWebServer.enqueue(new MockResponse().setBody("123").setHeader(CONTENT_TYPE, "internal/json"));

        var statusListener = mock(QueryStatusListener.class);
        client.addQueryStatusListener(statusListener);
        client.publishQuery(queryId);
        var recordedRequest = mockWebServer.takeRequest();

        assertEquals("codex/json", recordedRequest.getHeader(CONTENT_TYPE));
        assertEquals("internal/json", recordedRequest.getHeader(ACCEPT));
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("foo", recordedRequest.getBody().readUtf8());

        verify(statusListener, timeout(ASYNC_TIMEOUT_WAIT_MS)).onClientUpdate(queryId, "1", COMPLETED);

        assertEquals(1, client.getResultSiteIds(queryId).size());
        assertEquals("1", client.getResultSiteIds(queryId).get(0));
        assertEquals(123, client.getResultFeasibility(queryId, "1"));
    }

    @Test
    void testPublishQueryServerError() throws QueryNotFoundException, IOException {
        var queryId = client.createQuery();
        client.addQueryDefinition(queryId, STRUCTURED_QUERY, "foo");

        mockWebServer.enqueue(new MockResponse().setStatus(INTERNAL_SERVER_ERROR.toString()));

        var statusListener = mock(QueryStatusListener.class);
        client.addQueryStatusListener(statusListener);
        client.publishQuery(queryId);

        verify(statusListener, timeout(ASYNC_TIMEOUT_WAIT_MS).only()).onClientUpdate(queryId, "1", FAILED);
    }

    @Test
    void testPublishQueryUnexpectedResponseBody() throws QueryNotFoundException, IOException {
        var queryId = client.createQuery();
        client.addQueryDefinition(queryId, STRUCTURED_QUERY, "foo");

        mockWebServer.enqueue(new MockResponse().setBody("not-a-number").setHeader(CONTENT_TYPE, "internal/json"));

        var statusListener = mock(QueryStatusListener.class);
        client.addQueryStatusListener(statusListener);
        client.publishQuery(queryId);

        verify(statusListener, timeout(ASYNC_TIMEOUT_WAIT_MS).only()).onClientUpdate(queryId, "1", FAILED);
    }
}
