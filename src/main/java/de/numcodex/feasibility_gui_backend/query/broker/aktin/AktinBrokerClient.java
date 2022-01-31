package de.numcodex.feasibility_gui_backend.query.broker.aktin;

import de.numcodex.feasibility_gui_backend.query.broker.BrokerClient;
import de.numcodex.feasibility_gui_backend.query.broker.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.query.broker.SiteNotFoundException;
import de.numcodex.feasibility_gui_backend.query.broker.UnsupportedMediaTypeException;
import de.numcodex.feasibility_gui_backend.query.collect.QueryStatusListener;
import de.numcodex.feasibility_gui_backend.query.persistence.BrokerClientType;
import org.aktin.broker.client2.BrokerAdmin2;
import org.aktin.broker.xml.Node;
import org.aktin.broker.xml.RequestStatusInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.numcodex.feasibility_gui_backend.query.persistence.BrokerClientType.AKTIN;

/**
 * CODEX middleware controller implementation via AKTIN broker.
 *
 * @author R.W.Majeed
 *
 */
public class AktinBrokerClient implements BrokerClient {
	private final BrokerAdmin2 delegate;
	private final Map<String, Long> brokerToBackendQueryIdMapping;

    public AktinBrokerClient(BrokerAdmin2 delegate) {
        this.delegate = Objects.requireNonNull(delegate);
        this.brokerToBackendQueryIdMapping = new HashMap<>();
    }

    @Override
	public BrokerClientType getBrokerType() {
		return AKTIN;
	}

	@Override
	public void addQueryStatusListener(QueryStatusListener queryStatusListener) throws IOException {
		delegate.addListener(new WrappedNotificationListener(this, queryStatusListener));
		if( delegate.getWebsocket() == null ) {
			// only connect if previously not connected
			delegate.connectWebsocket(); // makes more sense to put in a separate method or in the constructor
		}
	}

	String wrapQueryId(int queryId) {
		return Integer.toString(queryId);
	}
	int unwrapQueryId(String queryId) {
		return Integer.parseInt(queryId);
	}
	String wrapSiteId(int siteId) {
		return Integer.toString(siteId);
	}
	int unwrapSiteId(String siteId) {
		return Integer.parseInt(siteId);
	}

	@Override
	public String createQuery(Long backendQueryId) throws IOException {
        var brokerQueryId = wrapQueryId(delegate.createRequest());
        brokerToBackendQueryIdMapping.put(brokerQueryId, backendQueryId);

        return brokerQueryId;
    }

	@Override
	public void addQueryDefinition(String brokerQueryId, String mediaType, String content)
			throws QueryNotFoundException, UnsupportedMediaTypeException, IOException {
		delegate.putRequestDefinition(unwrapQueryId(brokerQueryId), mediaType, content);

	}

	@Override
	public void publishQuery(String brokerQueryId) throws QueryNotFoundException, IOException {
		delegate.publishRequest(unwrapQueryId(brokerQueryId));
	}

	@Override
	public void closeQuery(String brokerQueryId) throws IOException {
		delegate.closeRequest(unwrapQueryId(brokerQueryId));
	}

	@Override
	public int getResultFeasibility(String brokerQueryId, String siteId)
			throws QueryNotFoundException, SiteNotFoundException, IOException {
		String result = delegate.getResultString(unwrapQueryId(brokerQueryId), unwrapSiteId(siteId));
		if( result == null ) {
			throw new SiteNotFoundException(brokerQueryId, siteId);
		}
		return Integer.parseInt(result);
	}

	@Override
	public List<String> getResultSiteIds(String brokerQueryId) throws QueryNotFoundException, IOException {
		List<RequestStatusInfo> list = delegate.listRequestStatus(unwrapQueryId(brokerQueryId));
		if( list == null ) {
			throw new QueryNotFoundException(brokerQueryId);
		}
		return list.stream()
				.map( (info) -> wrapSiteId(info.node) )
                .toList();
	}

	@Override
	public String getSiteName(String siteId) throws SiteNotFoundException, IOException {
		Node node = delegate.getNode(unwrapSiteId(siteId));
		if( node == null ) {
			throw new SiteNotFoundException(null, siteId);
		}
		return node.getCommonName();
	}

    Long getBackendQueryId(String brokerQueryId) {
        return brokerToBackendQueryIdMapping.get(brokerQueryId);
    }
}
