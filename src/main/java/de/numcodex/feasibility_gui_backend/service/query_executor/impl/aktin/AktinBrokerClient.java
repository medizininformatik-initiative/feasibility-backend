package de.numcodex.feasibility_gui_backend.service.query_executor.impl.aktin;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.aktin.broker.client2.BrokerAdmin2;
import org.aktin.broker.xml.Node;
import org.aktin.broker.xml.RequestStatusInfo;

import de.numcodex.feasibility_gui_backend.service.query_executor.BrokerClient;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryNotFoundException;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatusListener;
import de.numcodex.feasibility_gui_backend.service.query_executor.SiteNotFoundException;
import de.numcodex.feasibility_gui_backend.service.query_executor.UnsupportedMediaTypeException;
import lombok.AllArgsConstructor;

/**
 * CODEX middleware controller implementation via AKTIN broker.
 * 
 * @author R.W.Majeed
 *
 */
@AllArgsConstructor
public class AktinBrokerClient implements BrokerClient {
	private BrokerAdmin2 delegate;

	@Override
	public void addQueryStatusListener(QueryStatusListener queryStatusListener) throws IOException {
		delegate.openWebsocket(new WrappedNotificationListener(this, queryStatusListener));
	}

	String wrapQueryId(int queryId) {
		return Integer.toString(queryId);
	}
	int unwrapQueryId(String queryId) {
		return Integer.valueOf(queryId);
	}
	String wrapSiteId(int siteId) {
		return Integer.toString(siteId);
	}
	int unwrapSiteId(String siteId) {
		return Integer.valueOf(siteId);
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
	public void closeQuery(String queryId) throws QueryNotFoundException, IOException {
		delegate.closeRequest(unwrapQueryId(queryId));
	}

	@Override
	public int getResultFeasibility(String queryId, String siteId)
			throws QueryNotFoundException, SiteNotFoundException, IOException {
		String result = delegate.getResultString(unwrapQueryId(queryId), unwrapSiteId(siteId));
		if( result == null ) {
			throw new SiteNotFoundException(queryId, siteId);
		}
		return Integer.valueOf(result);
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
