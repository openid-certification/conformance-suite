package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationPARRequestParameters;
import net.openid.conformance.testmodule.Environment;

public class AustraliaConnectIdEnsureAuthorizationRequestContainsNoAcrClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY)
	public Environment evaluate(Environment env) {

		JsonElement acrClaim = env.getElementFromObject(CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY, "claims.id_token.acr");

		if (acrClaim != null && acrClaim.isJsonObject()) {
			// check value, values
			JsonObject acrObj = acrClaim.getAsJsonObject();
			JsonElement valueElement = acrObj.get("value");
			if(null != valueElement && valueElement.isJsonPrimitive()) {
				throw error("Authorization request claims object contains ID Token acr request", args("claims id_token acr value", acrClaim));
			}
			valueElement = acrObj.get("values");
			if(null != valueElement && valueElement.isJsonArray()) {
				throw error("Authorization request claims object contains ID Token acr request values", args("claims id_token acr values", acrClaim));
			}
		}

		JsonElement acrValues = env.getElementFromObject(CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY, "acr_values");
		if((null != acrValues) && acrValues.isJsonPrimitive()) {
			throw error("Authorization request contains acr_values", args("acr_values", acrValues));
		}

		logSuccess("Authorization request does not contain any acr requests");
		return env;
	}
}
