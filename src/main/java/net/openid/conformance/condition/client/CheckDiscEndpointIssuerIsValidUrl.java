package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.IssuerUrlValidation;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that the authorization server / OpenID Provider metadata {@code issuer} is a
 * well-formed RFC 8414 §2 issuer identifier: an {@code https} URL (scheme case-insensitive) with
 * a host component and no query, fragment, userinfo or out-of-range port.
 *
 * <p>This is structural validation of the identifier itself; {@link CheckDiscEndpointIssuer}
 * separately checks that the value matches the location the metadata was retrieved from.
 */
public class CheckDiscEndpointIssuerIsValidUrl extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonElement issuerEl = env.getElementFromObject("server", "issuer");
		if (!OIDFJSON.isString(issuerEl)) {
			throw error("issuer is missing or not a string in the authorization server metadata",
				args("issuer", issuerEl));
		}
		String issuer = OIDFJSON.getString(issuerEl);

		List<String> issues = new ArrayList<>();
		IssuerUrlValidation.validate(issuer, "issuer", issues);
		if (!issues.isEmpty()) {
			throw error("issuer is not a valid RFC 8414 issuer identifier URL",
				args("issuer", issuer, "issues", issues));
		}

		logSuccess("issuer is a valid issuer identifier URL", args("issuer", issuer));
		return env;
	}
}
