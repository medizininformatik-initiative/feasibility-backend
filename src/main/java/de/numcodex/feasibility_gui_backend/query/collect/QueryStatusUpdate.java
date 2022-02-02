package de.numcodex.feasibility_gui_backend.query.collect;

import de.numcodex.feasibility_gui_backend.query.broker.BrokerClient;

/**
 * Defines a status update on a broker specific query.
 * <p>
 * Comprises information about the broker that the update originates from, the associated broker specific query ID, the
 * associated broker specific site ID as well as the query status itself.
 */
public record QueryStatusUpdate(BrokerClient source, String brokerQueryId, String brokerSiteId,
                                QueryStatus status) {
}
