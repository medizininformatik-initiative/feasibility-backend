package de.numcodex.feasibility_gui_backend.service.query_executor.impl.dsf;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Holds information about an optional proxy used to communicate with a FHIR server.
 */
@Data
@AllArgsConstructor
public class FhirProxyContext {
    String proxyHost;
    String username;
    String password;
}
