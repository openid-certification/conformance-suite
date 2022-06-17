package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.rs.OIDCCLoadUserInfo;
import net.openid.conformance.testmodule.Environment;

public class LoadRequestedIdTokenClaims extends AbstractCondition {

	protected static JsonObject userinfoValues = OIDCCLoadUserInfo.getUserInfoClaimsValues();

	@Override
	@PreEnvironment(required = { CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY, "id_token_claims" })
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonElement requesteIdTokenClaimsElement = env.getElementFromObject(CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY, "claims.id_token");
		if((null != requesteIdTokenClaimsElement) && requesteIdTokenClaimsElement.isJsonObject()) {
			JsonObject requesteIdTokenClaims = requesteIdTokenClaimsElement.getAsJsonObject();
			JsonObject idTokenClaims = env.getObject("id_token_claims");

			for (String key: requesteIdTokenClaims.keySet()) {
				setIdTokenClaim(idTokenClaims, key, requesteIdTokenClaims.get(key), false);
			}
			logSuccess("Added requested claims to ID Token", args("requested ID Token claims", requesteIdTokenClaims, "Claims returned", idTokenClaims));
		} else {
			logSuccess("No ID Token claims requested");
		}
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
			} else if(claimRequestValue.isJsonObject()) {
				JsonObject claimRequestObject = claimRequestValue.getAsJsonObject();
				JsonElement essential = claimRequestObject.get("essential");
				boolean isEssential = (essential != null) && essential.getAsBoolean();
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
