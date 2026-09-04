package net.openid.conformance.openid.ssf.conditions.events;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Asserts that the receiver answered a push delivery of a deliberately invalid
 * SET with an error response. RFC 8935 2.2 reserves 202 for successful
 * transmission; 2.3 requires a failed validation to be answered with an error
 * response - acknowledging an invalid SET (any 2xx) fails this check.
 */
public class OIDSSFEnsureReceiverRejectedPushDelivery extends AbstractCondition {

	protected final String invalidSetDescription;

	public OIDSSFEnsureReceiverRejectedPushDelivery(String invalidSetDescription) {
		this.invalidSetDescription = invalidSetDescription;
	}

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		int status = env.getInteger("endpoint_response", "status");

		if (status >= 200 && status < 300) {
			throw error("Receiver accepted an invalid SET (" + invalidSetDescription + ") via push delivery. "
					+ "Invalid SETs must be rejected with an error response (RFC 8935 2.3).",
				args("http_status", status));
		}

		if (status == 0) {
			throw error("No HTTP response was received for the push delivery of an invalid SET (" + invalidSetDescription + "). "
					+ "The receiver must reject invalid SETs with an error response (RFC 8935 2.3), not by dropping the connection.",
				args("http_status", status));
		}

		if (status < 400) {
			// 1xx/3xx: a redirect or informational response is not a rejection
			throw error("Receiver answered the push delivery of an invalid SET (" + invalidSetDescription + ") with HTTP " + status
					+ ", which is not an error response. Invalid SETs must be rejected with an error response (RFC 8935 2.3).",
				args("http_status", status));
		}

		logSuccess("Receiver rejected the invalid SET (" + invalidSetDescription + ") via push delivery",
			args("http_status", status, "body", env.getElementFromObject("endpoint_response", "body_json")));

		return env;
	}
}
