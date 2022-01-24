package de.numcodex.feasibility_gui_backend.query.broker.aktin;

import de.numcodex.feasibility_gui_backend.query.broker.BrokerClient;
import de.numcodex.feasibility_gui_backend.query.broker.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.query.broker.SiteNotFoundException;
import de.numcodex.feasibility_gui_backend.query.broker.UnsupportedMediaTypeException;
import de.numcodex.feasibility_gui_backend.query.collect.QueryStatusListener;
import de.numcodex.feasibility_gui_backend.query.persistence.BrokerClientType;
import lombok.AllArgsConstructor;
import org.aktin.broker.client2.BrokerAdmin2;
import org.aktin.broker.xml.Node;
import org.aktin.broker.xml.RequestStatusInfo;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static de.numcodex.feasibility_gui_backend.query.persistence.BrokerClientType.AKTIN;

/**
 * CODEX middleware controller implementation via AKTIN broker.
 *
 * @author R.W.Majeed
 *
 */
@AllArgsConstructor
public class AktinBrokerClient implements BrokerClient {
	final private BrokerAdmin2 delegate;

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
	public String createQuery() throws IOException {
		return wrapQueryId(delegate.createRequest());
	}

	@Override
	public void addQueryDefinition(String queryId, String mediaType, String content)
			throws QueryNotFoundException, UnsupportedMediaTypeException, IOException {
		delegate.putRequestDefinition(unwrapQueryId(queryId), mediaType, content);

	}

	@Override
	public void publishQuery(String queryId) throws QueryNotFoundException, IOException {
		delegate.publishRequest(unwrapQueryId(queryId));
	}

	@Override
	public void closeQuery(String queryId) throws IOException {
		delegate.closeRequest(unwrapQueryId(queryId));
	}

	@Override
	public int getResultFeasibility(String queryId, String siteId)
			throws QueryNotFoundException, SiteNotFoundException, IOException {
		String result = delegate.getResultString(unwrapQueryId(queryId), unwrapSiteId(siteId));
		if( result == null ) {
			throw new SiteNotFoundException(queryId, siteId);
		}
		return Integer.parseInt(result);
	}

	@Override
	public List<String> getResultSiteIds(String queryId) throws QueryNotFoundException, IOException {
		List<RequestStatusInfo> list = delegate.listRequestStatus(unwrapQueryId(queryId));
		if( list == null ) {
			throw new QueryNotFoundException(queryId);
		}
		return list.stream()
				.map( (info) -> wrapSiteId(info.node) )
				.collect(Collectors.toUnmodifiableList());
	}

	@Override
	public String getSiteName(String siteId) throws SiteNotFoundException, IOException {
		Node node = delegate.getNode(unwrapSiteId(siteId));
		if( node == null ) {
			throw new SiteNotFoundException(null, siteId);
		}
		return node.getCommonName();
	}

}
