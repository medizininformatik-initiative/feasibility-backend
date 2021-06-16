package de.numcodex.feasibility_gui_backend.service.query_executor.impl.aktin;

import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatus;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatusListener;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;

import java.util.logging.Level;

import org.aktin.broker.client2.AdminNotificationListener;
import org.aktin.broker.xml.RequestStatus;

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
@Log
public class WrappedNotificationListener implements AdminNotificationListener{
	final private AktinBrokerClient client;
	final private QueryStatusListener delegate;

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
		
		dest = switch (rs) {
			case completed -> QueryStatus.COMPLETED;
			case processing -> QueryStatus.EXECUTING;
			case queued -> QueryStatus.QUEUED;
			case retrieved -> QueryStatus.RETRIEVED;
			default -> QueryStatus.FAILED;
		};
		log.log(Level.INFO,"Request status updated {0} {1} {2}", new Object[] {requestId,nodeId,status});
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

	@Override
	public void onWebsocketClosed(int statusCode) {
		; // NOP
	}

}
