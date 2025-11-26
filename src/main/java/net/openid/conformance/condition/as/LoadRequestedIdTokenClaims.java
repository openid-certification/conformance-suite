package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AustraliaConnectIdCheckTrustFrameworkSupported;
import net.openid.conformance.condition.client.AustraliaConnectIdCheckVerifiedClaimsSupported;
import net.openid.conformance.condition.rs.OIDCCLoadUserInfo;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class LoadRequestedIdTokenClaims extends AbstractCondition {

	protected static JsonObject getUserInfoVerifiedClaimsValues() {

		JsonObject verified = new JsonObject();

		for (String claim : AustraliaConnectIdCheckVerifiedClaimsSupported.ConnectIdVerifiedClaims) {
			switch (claim) {
				case "over16": {
					verified.addProperty("over16" ,true);
					break;
				}
				case "over18": {
					verified.addProperty("over18" ,true);
					break;
				}
				case "over21": {
					verified.addProperty("over21" ,true);
					break;
				}
				case "over25": {
					verified.addProperty("over25" ,true);
					break;
				}
				case "over65": {
					verified.addProperty("over65" ,true);
					break;
				}
				case "beneficiary_account_au": {
					JsonObject beneficiary = new JsonObject();
					beneficiary.addProperty("beneficiary_name", "John Smith");
					beneficiary.addProperty("account_bsb", "100200");
					beneficiary.addProperty("account_number", "12345678");
					verified.add("beneficiary_account_au", beneficiary);
					break;
				}
				case "beneficiary_account_au_payid": {
					JsonObject beneficiary = new JsonObject();
					beneficiary.addProperty("beneficiary_name", "John Smith");
					beneficiary.addProperty("payid", "0400000321");
					beneficiary.addProperty("payid_type", "/TELI");
					verified.add("beneficiary_account_au_payid", beneficiary);
					break;
				}
				case "beneficiary_account_international": {
					JsonObject beneficiary = new JsonObject();
					beneficiary.addProperty("beneficiary_name", "John Smith");
					beneficiary.addProperty("bic_swift_code", "XXXXXNNNNX");
					beneficiary.addProperty("account_number_international", "10020012345678");
					beneficiary.addProperty("beneficiary_residential_address", "255 George St, Sydney, NSW 2000, Australia");
					verified.add("beneficiary_account_international", beneficiary);
					break;
				}
				default: {
					break;
				}
			}
		}

		return verified;
	}

	protected static JsonObject userinfoValues = OIDCCLoadUserInfo.getUserInfoClaimsValues();
	protected static JsonObject verifiedValues = getUserInfoVerifiedClaimsValues();

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
			if (key.equals("verified_claims")) {

				JsonElement verifiedClaimsElement = requestedIdTokenClaims.getAsJsonObject("verified_claims").get("claims");

				if (verifiedClaimsElement != null) {
					for (String key1: verifiedClaimsElement.getAsJsonObject().keySet()) {
						setIdTokenVerifiedClaim(idTokenClaims, key1, requestedIdTokenClaims.get(key1), false);
					}
				}

				JsonElement verificationElement = requestedIdTokenClaims.getAsJsonObject("verified_claims").get("verification");
				if (verificationElement != null) {
					if (idTokenClaims.get("verified_claims") == null) {
						idTokenClaims.add("verified_claims",  new JsonObject());
					}

					JsonObject trustFramework = new JsonObject();
					trustFramework.addProperty("trust_framework", AustraliaConnectIdCheckTrustFrameworkSupported.ConnectIdTrustFramework);

					idTokenClaims.getAsJsonObject("verified_claims").add("verification", trustFramework);
				}

				continue;
			}

			setIdTokenClaim(idTokenClaims, key, requestedIdTokenClaims.get(key), userinfoValues, false);
		}
		logSuccess("Added requested claims to ID Token", args("requested ID Token claims", requestedIdTokenClaims, "Claims returned", idTokenClaims));

		return env;
	}

	/***
	 * Fills in the ID Token with the requested verified claims value
	 * @param idTokenClaims The ID Token JsonObject to set
	 * @param claimRequestName Name of the requested ID Token claim
	 * @param claimRequestValue The ID Token Claim Request Value
	 *			   e.g. null
	 *			   {"essential" : true}
	 *			   {"value" : "foo"}
	 *			  {"values" : "blah1", "blah2"}
	 * @param essentialOnly Whether to return essential claims only
	 */
	protected void setIdTokenVerifiedClaim(JsonObject idTokenClaims, String claimRequestName, JsonElement claimRequestValue, boolean essentialOnly) {
		JsonElement claimValue = verifiedValues.get(claimRequestName);

		if(null != claimValue) {
			if (idTokenClaims.get("verified_claims") == null) {
				idTokenClaims.add("verified_claims",  new JsonObject());
			}

			if (idTokenClaims.getAsJsonObject("verified_claims").get("claims") == null) {
				idTokenClaims.getAsJsonObject("verified_claims").add("claims",  new JsonObject());
			}

			setIdTokenClaim(idTokenClaims.getAsJsonObject("verified_claims").getAsJsonObject("claims"), claimRequestName, claimRequestValue, verifiedValues, essentialOnly);
		}
	}

	/***
	 * Fills in the ID Token with the requested claims value
	 * @param idTokenClaims The ID Token JsonObject to set
	 * @param claimRequestName Name of the requested ID Token claim
	 * @param claimRequestValue The ID Token Claim Request Value
	 *			   e.g. null
	 *			   {"essential" : true}
	 *			   {"value" : "foo"}
	 *			  {"values" : "blah1", "blah2"}
	 * @param claimValues JsonObject containing template claim values
	 * @param essentialOnly Whether to return essential claims only
	 */
	protected void setIdTokenClaim(JsonObject idTokenClaims, String claimRequestName, JsonElement claimRequestValue, JsonObject claimValues, boolean essentialOnly) {
		JsonElement claimValue = claimValues.get(claimRequestName);

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
