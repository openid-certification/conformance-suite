package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Arrays;
import java.util.List;

public class ValidateIdTokenACRClaimAgainstAcrValuesRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "id_token",  "authorization_endpoint_request" })
	public Environment evaluate(Environment env) {
		// Read what the server has sent us
		JsonElement idTokenAcrClaim = env.getElementFromObject("id_token", "claims.acr");

		JsonElement requestedAcrValues = env.getElementFromObject("authorization_endpoint_request", "acr_values");
		if (requestedAcrValues == null) {
			throw error("authorization endpoint request did not contain acr_values; this is a problem with the conformance suite");
		}

		if (idTokenAcrClaim == null) {
			throw error("An acr value was requested using acr_values, so the server 'SHOULD' return an acr claim, but it did not.",
				args("request", env.getObject("authorization_endpoint_request"),
					"id_token", env.getObject("id_token")));
		}

		if (!idTokenAcrClaim.isJsonPrimitive() || !idTokenAcrClaim.getAsJsonPrimitive().isString()) {
			throw error("acr value in id_token must be a string", args("id_token", env.getObject("id_token")));
		}

		String idTokenAcr = OIDFJSON.getString(idTokenAcrClaim);

		List<String> requestedValues = Arrays.asList(OIDFJSON.getString(requestedAcrValues).split(" "));

		if (requestedValues.contains(idTokenAcr)) {
			logSuccess("id_token acr claim contains one of the requested values",
				args("requested_values", requestedValues, "id_token_acr", idTokenAcr));
			return env;
		}

		throw error("acr value in id_token is not (one of the) requested values",
			args("requested_values", requestedValues, "id_token_acr", idTokenAcr));
	}
}
