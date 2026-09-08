package net.openid.conformance.openid.ssf.conditions.events;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;
import java.util.Locale;

/**
 * RFC 8935 2.1: "The Accept header field MUST be application/json, as any TLS errors would be
 * conveyed with that content type". A list value containing application/json (with optional
 * media type parameters such as q-values) is tolerated.
 */
public class OIDSSFEnsurePushRequestAcceptHeaderIncludesJson extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		String accept = env.getString("ssf", "push_request.headers.accept");
		if (accept == null) {
			throw error("Push delivery request has no Accept header. RFC 8935 requires 'application/json'.",
				args("headers", env.getElementFromObject("ssf", "push_request.headers")));
		}

		boolean acceptsJson = Arrays.stream(accept.split(","))
			.map(value -> value.split(";", 2)[0].trim().toLowerCase(Locale.ROOT))
			.anyMatch("application/json"::equals);

		if (!acceptsJson) {
			throw error("Push delivery request Accept header does not include 'application/json'",
				args("accept", accept));
		}

		logSuccess("Push delivery request Accept header includes 'application/json'", args("accept", accept));

		return env;
	}
}
