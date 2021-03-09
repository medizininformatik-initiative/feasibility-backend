package de.numcodex.feasibility_gui_backend.service.query_executor.impl.aktin;

import org.aktin.broker.client2.AdminNotificationListener;
import org.aktin.broker.xml.RequestStatus;

import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatus;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatusListener;
import lombok.AllArgsConstructor;

/**
 * Wraps the CODEX notification listener with AKTIN admin notification listener
 * and translates status update notifications.
 *
 * Any unexpected status states are treated as failed.
 *
 * @author R.W.Majeed
 *
 */
@AllArgsConstructor
public class WrappedNotificationListener implements AdminNotificationListener{
	private AktinBrokerClient client;
	private QueryStatusListener delegate;

	@Override
	public void onRequestCreated(int requestId) {
		; // NOP
	}

	@Override
	public void onRequestPublished(int requestId) {
		; // NOP
	}

	@Override
	public void onRequestClosed(int requestId) {
		; // NOP
	}

	@Override
	public void onRequestStatusUpdate(int requestId, int nodeId, String status) {
		QueryStatus dest;
		RequestStatus rs;
		try{
			rs = RequestStatus.valueOf(status);
		}catch( IllegalArgumentException e ) {
			return; // unsupported/unrecognized status
		}
		
		switch( rs ) {
		case completed:
			dest = QueryStatus.COMPLETED;
			break;
		case processing:
			dest = QueryStatus.EXECUTING;
			break;
		case queued:
			dest = QueryStatus.QUEUED;
			break;
		case retrieved:
			dest = QueryStatus.RETRIEVED;
			break;
		case expired:
		case failed:
		case rejected:
		case interaction:
		default:
			dest = QueryStatus.FAILED;
			break;			
		}
		delegate.onClientUpdate(client.wrapQueryId(requestId), client.wrapSiteId(nodeId), dest);
	}

	@Override
	public void onRequestResultUpdate(int requestId, int nodeId, String mediaType) {
		; // NOP
	}

	@Override
	public void onResourceUpdate(int nodeId, String resourceId) {
		; // NOP
	}

	// TODO implement reconnect on close with updated broker-client dependency
}
