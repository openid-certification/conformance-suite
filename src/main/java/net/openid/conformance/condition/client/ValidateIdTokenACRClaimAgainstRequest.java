package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;

public class ValidateIdTokenACRClaimAgainstRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "id_token",  "authorization_endpoint_request" })
	public Environment evaluate(Environment env) {

		// Read what the server has sent us
		JsonElement idTokenAcrClaim = env.getElementFromObject("id_token", "claims.acr");

		// Do validation; regardless of what we requested if an acr is returned it must be a string
		if (idTokenAcrClaim != null) {
			if (!idTokenAcrClaim.isJsonPrimitive() || !idTokenAcrClaim.getAsJsonPrimitive().isString()) {
				throw error("acr value in id_token must be a string", args("id_token", env.getObject("id_token")));
			}
		}

		JsonElement requestedAcrValue = env.getElementFromObject("authorization_endpoint_request", "claims.id_token.acr.value");
		JsonElement requestedAcrValues = env.getElementFromObject("authorization_endpoint_request", "claims.id_token.acr.values");
		ArrayList<String> requestedValues = new ArrayList<>();
		if (requestedAcrValues != null) {
			for (JsonElement value : requestedAcrValues.getAsJsonArray()) {
				String val = OIDFJSON.getString(value);
				requestedValues.add(val);
			}
		} else if (requestedAcrValue != null) {
			requestedValues.add(OIDFJSON.getString(requestedAcrValue));
		} else {
			logSuccess("Nothing to check; the conformance suite did not request an acr claim in request object");
			return env;
		}

		if (idTokenAcrClaim == null) {
			throw error("One or more acr values were requested as an 'essential: true' claim so, as the authentication succeeded, the acr used MUST be returned in the id_token",
				args("id_token", env.getObject("id_token"), "expected", requestedValues));
		}

		String idTokenValue = OIDFJSON.getString(idTokenAcrClaim);

		for (String singleAcrValue : requestedValues) {
			if (idTokenValue.equals(singleAcrValue)) {
				logSuccess("acr value in id_token is (one of) the requested values", args("requested", requestedValues, "actual", idTokenValue));
				return env;
			}
		}

		throw error("acr value in id_token is not (one of the) requested values", args("requested", requestedValues, "actual", idTokenValue));
	}
}
