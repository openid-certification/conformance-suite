package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

public class FAPIBrazilValidateIdTokenACRClaim extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "id_token",  "authorization_endpoint_request" })
	public Environment evaluate(Environment env) {
		List<String> permitted = List.of("urn:brasil:openbanking:loa2", "urn:brasil:openbanking:loa3");

		// Read what the server has sent us
		JsonElement idTokenAcrClaim = env.getElementFromObject("id_token", "claims.acr");

		if (idTokenAcrClaim == null) {
			throw error("The acr used MUST be returned in the id_token but no acr claim was found",
				args("id_token", env.getObject("id_token"), "permitted", permitted));
		}

		// Do validation; regardless of what we requested if an acr is returned it must be a string
		if (!idTokenAcrClaim.isJsonPrimitive() || !idTokenAcrClaim.getAsJsonPrimitive().isString()) {
			throw error("acr value in id_token must be a string", args("id_token", env.getObject("id_token")));
		}

		String idTokenValue = OIDFJSON.getString(idTokenAcrClaim);

		for (String singleAcrValue : permitted) {
			if (idTokenValue.equals(singleAcrValue)) {
				logSuccess("acr value in id_token is one of the permitted values", args("permitted", permitted, "actual", idTokenValue));
				return env;
			}
		}

		throw error("acr value in id_token is not one of the permitted values", args("permitted", permitted, "actual", idTokenValue));
	}
}
