package de.numcodex.feasibility_gui_backend.query.result;

import de.numcodex.feasibility_gui_backend.query.persistence.ResultType;
import java.util.Objects;

/**
 * Represents one submitted result from one connected site.
 * <p>
 * A ResultLine is received via one of the connected
 * {@link de.numcodex.feasibility_gui_backend.query.broker.BrokerClient BrokerClient}
 * implementations. It holds information about the site name (as submitted by the
 * {@code BrokerClient}, the {@link ResultType} (success or error)and the number of patients.
 */
public record ResultLine(String siteName, ResultType type, long result) {

  public ResultLine {
    Objects.requireNonNull(siteName);
    Objects.requireNonNull(type);
  }
}
