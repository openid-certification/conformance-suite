package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;
import java.util.Set;

public class CdrCheckForUnexpectedClaimsInIntrospectionResponse extends AbstractCondition {

	private static final Set<String> KNOWN_CLAIMS = Set.of(
		// defined in RFC7662 section 2.2 (username is listed there too, but the CDR
		// standards prohibit it; a separate condition fails if it is present)
		"active", "scope", "client_id", "username", "token_type", "exp", "iat", "nbf",
		"sub", "aud", "iss", "jti",
		// defined by the CDR standards
		"cdr_arrangement_id");

	@Override
	@PreEnvironment(required = CallTokenIntrospectionEndpoint.RESPONSE_KEY)
	public Environment evaluate(Environment env) {

		JsonObject body = env.getElementFromObject(CallTokenIntrospectionEndpoint.RESPONSE_KEY, "body_json").getAsJsonObject();

		List<String> unexpected = body.keySet().stream().filter(k -> !KNOWN_CLAIMS.contains(k)).toList();

		if (!unexpected.isEmpty()) {
			throw error("The introspection response contains claims that are defined neither in RFC7662 nor by the CDR standards. RFC7662 permits extension claims so this may be deliberate, but it may also indicate a misspelt claim name.",
				args("unexpected_claims", unexpected));
		}

		logSuccess("The introspection response contains only claims defined in RFC7662 or by the CDR standards");
		return env;
	}

}
