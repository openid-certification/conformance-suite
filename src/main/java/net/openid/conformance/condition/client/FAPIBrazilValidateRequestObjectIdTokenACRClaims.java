package net.openid.conformance.condition.client;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

public class FAPIBrazilValidateRequestObjectIdTokenACRClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {


		String scope = env.getString("authorization_request_object", "claims.scope");
		List<String> scopes = Lists.newArrayList(Splitter.on(" ").split(scope).iterator());

		JsonElement acrClaim = env.getElementFromObject("authorization_request_object", "claims.claims.id_token.acr");
		if (acrClaim == null) {
				throw error("Client has not requested the acr claim");

		}
		if (!acrClaim.isJsonObject()) {
			throw error("The acr claim is not a JsonObject", args("acrClaim", acrClaim));
		}

		if (acrClaim.getAsJsonObject().has("essential") == false ||
			OIDFJSON.getBoolean(acrClaim.getAsJsonObject().get("essential")) != true) {
			throw error("Client has not requested the acr claim as an 'essential' claim", args("acrClaim", acrClaim));
		}


		// https://openid.net/specs/openid-connect-core-1_0.html#acrSemantics
		if (acrClaim.getAsJsonObject().has("values")) {
			throw error("Client has requested 'values' for the acr claim", args("acrClaim", acrClaim));
		} else if (acrClaim.getAsJsonObject().has("value")) {
			throw error("Client has requested 'value' for the acr claim", args("acrClaim", acrClaim));
		} else {
			// ensure we return an acr value, even though there was no request for an explicit value
			JsonArray matchedAcrValues = new JsonArray();
			if (scopes.contains("customers")) {
				matchedAcrValues.add("urn:brasil:openinsurance:loa2");
			} else {
				matchedAcrValues.add("urn:brasil:openbanking:loa3");
			}
			env.putString("requested_id_token_acr_values", matchedAcrValues.toString());
			logSuccess("Acr value in request object is as expected");
			return env;
		}
	}
}
