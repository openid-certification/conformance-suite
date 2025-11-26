package net.openid.conformance.info;

import java.util.Optional;

/**
 * Service for retrieving Playwright trace files from test runs.
 */
public interface TraceService {

	/**
	 * Get the Playwright trace for a test.
	 *
	 * @param testId The test instance ID
	 * @param assumeAdmin If true, no access controls will be applied. Only set to true if being called from the
	 *                    test module itself, not via the REST API.
	 * @return Optional containing the trace bytes (ZIP file), or empty if no trace exists
	 */
	Optional<byte[]> getTraceForTestId(String testId, boolean assumeAdmin);
}
