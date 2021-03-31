package de.numcodex.feasibility_gui_backend.service.query_executor.impl.aktin;

import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatus;
import de.numcodex.feasibility_gui_backend.service.query_executor.QueryStatusListener;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;

import java.io.IOException;
import java.net.http.WebSocket;
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
@Log
@AllArgsConstructor
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
		// try to reconnect
		WebSocket ws = null;
		while( ws == null ) {
			try {
				ws = client.connectWebsocket();
				log.info("Websocket connection re-established.");
				// connection successful.
			}catch( IOException e ) {
				// unable to connect
				log.log(Level.WARNING, "Unable to reconnect closed websocket: "+e.getMessage()); 
			}
			if( ws == null ) {
				// connection failed, try again after delay
				// note that we are in a separate thread provided by the AKTIN client library for websocket callbacks
				// therefore we can block here without breaking anything else
				log.info("Waiting for next try to re-connect websocket in 10s");
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					// interruption can be early, but we don't care to connect earlier
				}
			}
		}
	}

}
