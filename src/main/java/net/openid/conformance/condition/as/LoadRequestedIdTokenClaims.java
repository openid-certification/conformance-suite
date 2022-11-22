package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.rs.OIDCCLoadUserInfo;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class LoadRequestedIdTokenClaims extends AbstractCondition {

	protected static JsonObject userinfoValues = OIDCCLoadUserInfo.getUserInfoClaimsValues();

	@Override
	@PreEnvironment(required = { CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY, "id_token_claims" })
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonElement requestedIdTokenClaimsElement = env.getElementFromObject(CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY, "claims.id_token");

		if (requestedIdTokenClaimsElement == null) {
			logSuccess("No ID Token claims requested");
			return env;
		}

		if (!requestedIdTokenClaimsElement.isJsonObject()) {
			throw error("claims.id_token.claims in request must be a JSON object",
				args("claims", requestedIdTokenClaimsElement));
		}

		JsonObject requestedIdTokenClaims = requestedIdTokenClaimsElement.getAsJsonObject();
		JsonObject idTokenClaims = env.getObject("id_token_claims");

		for (String key: requestedIdTokenClaims.keySet()) {
			setIdTokenClaim(idTokenClaims, key, requestedIdTokenClaims.get(key), false);
		}
		logSuccess("Added requested claims to ID Token", args("requested ID Token claims", requestedIdTokenClaims, "Claims returned", idTokenClaims));

		return env;
	}

	/***
	 * Fills in the ID Token with the requested claims value
	 * @param idTokenClaims The ID Token JsonObject to set
	 * @param claimRequestName Name of the requested ID Token claim
	 * @param claimRequestValue The ID Token Claim Request Value
	 *                           e.g. null
	 *                           {"essential" : true}
	 *                           {"value" : "foo"}
	 *                          {"values" : "blah1", "blah2"}
	 * @param essentialOnly Whether to return essential claims only
	 */
	protected void setIdTokenClaim(JsonObject idTokenClaims, String claimRequestName, JsonElement claimRequestValue, boolean essentialOnly) {
		JsonElement claimValue = userinfoValues.get(claimRequestName);

		if(null != claimValue) {
			if(((null == claimRequestValue) || claimRequestValue.isJsonNull()) && !essentialOnly) {
				idTokenClaims.add(claimRequestName, claimValue);
			} else if((null != claimRequestValue) && claimRequestValue.isJsonObject()) {
				JsonObject claimRequestObject = claimRequestValue.getAsJsonObject();
				JsonElement essential = claimRequestObject.get("essential");
				boolean isEssential = (essential != null) && OIDFJSON.getBoolean(essential);
				if(!essentialOnly || isEssential) {
					JsonElement requestedValue = claimRequestObject.get("value");
					JsonElement requestedValues = claimRequestObject.get("values");

					// fill values with the requested value???
					if(null != requestedValue) {
						idTokenClaims.add(claimRequestName, requestedValue);
					} else if(null != requestedValues){
						JsonArray requestedValuesArray = requestedValues.getAsJsonArray();
						if(!requestedValuesArray.isEmpty()) {
							idTokenClaims.add(claimRequestName, requestedValuesArray.get(0));
						}
					} else {
						idTokenClaims.add(claimRequestName, claimValue);
					}
				}
			}
		}
	}

}
