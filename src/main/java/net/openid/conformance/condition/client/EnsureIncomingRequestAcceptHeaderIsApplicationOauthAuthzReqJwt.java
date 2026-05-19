package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Per OID4VP 1.0 Final §5.10, the wallet's POST request to the verifier's request_uri MUST set
 * the Accept header to {@code application/oauth-authz-req+jwt}.
 */
public class EnsureIncomingRequestAcceptHeaderIsApplicationOauthAuthzReqJwt extends AbstractCondition {

	private static final String EXPECTED = "application/oauth-authz-req+jwt";

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {
		String accept = env.getString("incoming_request", "headers.accept");

		if (Strings.isNullOrEmpty(accept)) {
			throw error("Incoming request does not have an Accept header");
		}

		// Accept may be a comma-separated list with optional ;q= and other parameters per RFC 9110.
		// Match on the bare media type for at least one entry.
		for (String part : accept.split(",")) {
			String mimeType = AbstractCheckEndpointContentTypeReturned.getMimeTypeFromContentType(part);
			if (EXPECTED.equalsIgnoreCase(mimeType)) {
				logSuccess("Incoming Accept header includes the expected media type",
					args("accept", accept, "expected", EXPECTED));
				return env;
			}
		}

		throw error("Incoming Accept header does not include the expected media type",
			args("accept", accept, "expected", EXPECTED));
	}
}
