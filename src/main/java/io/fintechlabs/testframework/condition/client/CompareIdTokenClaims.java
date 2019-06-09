package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CompareIdTokenClaims extends AbstractCondition {
	private static final String CLAIM_AUTH_TIME = "auth_time";
	private static final String CLAIM_IAT = "iat";
	private static final String CLAIM_AZP = "azp";

	@Override
	@PreEnvironment(required = {"first_id_token_claims", "second_id_token_claims"})
	public Environment evaluate(Environment env) {
		if (!env.containsObject("first_id_token_claims")) {
			throw error("firstIdToken is not set");
		}

		if (!env.containsObject("second_id_token_claims")) {
			throw error("secondIdToken is not set");
		}
		JsonObject firstIdToken = env.getObject("first_id_token_claims");
		JsonObject secondIdToken = env.getObject("second_id_token_claims");

		if(firstIdToken==null) {
			throw error("Initial id token is null");
		}
		if(secondIdToken==null) {
			throw error("Second id token, which should have been obtained by a refresh_token call, is null");
		}
		JsonObject valuesForLog = new JsonObject();
		ensureClaimsExistAndAreEqual(firstIdToken, secondIdToken, "iss", valuesForLog);
		ensureClaimsExistAndAreEqual(firstIdToken, secondIdToken, "sub", valuesForLog);
		checkIssuedAt(firstIdToken, secondIdToken, valuesForLog);
		ensureClaimsExistAndAreEqual(firstIdToken, secondIdToken, "aud", valuesForLog);
		checkAuthTime(firstIdToken, secondIdToken, valuesForLog);
		checkAzp(firstIdToken, secondIdToken, valuesForLog);

		logSuccess("Validated id token claims successfully", valuesForLog);
		return env;
	}

	/**
	 * its azp Claim Value MUST be the same as in the ID Token issued when the original authentication occurred;
	 * if no azp Claim was present in the original ID Token, one MUST NOT be present in the new ID Token
	 * @param firstIdToken
	 * @param secondIdToken
	 */
	private void checkAzp(JsonObject firstIdToken, JsonObject secondIdToken, JsonObject valuesForLog) {
		if (!firstIdToken.has(CLAIM_AZP)) {
			if(secondIdToken.has(CLAIM_AZP)) {
				throw error("Second id token cannot contain an azp claim because the initial id token does not have an azp claim");
			}
		}
		if (!firstIdToken.has(CLAIM_AZP) && !secondIdToken.has(CLAIM_AZP)) {
			valuesForLog.add(CLAIM_AZP, new JsonPrimitive("Id tokens do not contain " + CLAIM_AZP + " claims"));
			return;
		}
		JsonPrimitive claim1 = firstIdToken.getAsJsonPrimitive(CLAIM_AZP);
		JsonPrimitive claim2 = secondIdToken.getAsJsonPrimitive(CLAIM_AZP);
		if(claim2!=null && !claim2.equals(claim1)) {
			throw error(CLAIM_AZP + " claims are not the same", args("claim1", claim1, "claim2", claim2));
		}
		JsonObject values = new JsonObject();
		values.addProperty("first", claim1.getAsString());
		values.addProperty("second", claim2.getAsString());
		values.addProperty("note", "Values are expected to be equal");
		valuesForLog.add(CLAIM_AZP, values);
	}

	/**
	 * if the ID Token contains an auth_time Claim,
	 * its value MUST represent the time of the original authentication
	 * - not the time that the new ID token is issued,
	 * TODO what if the second id token contains an auth_time claim but not the first id token
	 * @param firstIdToken
	 * @param secondIdToken
	 */
	private void checkAuthTime(JsonObject firstIdToken, JsonObject secondIdToken, JsonObject valuesForLog) {
		if (!secondIdToken.has(CLAIM_AUTH_TIME)) {
			return;
		}
		JsonPrimitive claim1 = firstIdToken.getAsJsonPrimitive(CLAIM_AUTH_TIME);
		JsonPrimitive claim2 = secondIdToken.getAsJsonPrimitive(CLAIM_AUTH_TIME);
		if(!claim2.equals(claim1)) {
			throw error("auth_time claims are not the same", args("claim1", claim1, "claim2", claim2));
		}
		JsonObject values = new JsonObject();
		values.addProperty("first", claim1.getAsString());
		values.addProperty("second", claim2.getAsString());
		values.addProperty("note", "Values are expected to be equal");
		valuesForLog.add(CLAIM_AUTH_TIME, values);
	}

	/**
	 * its iat Claim MUST represent the time that the new ID Token is issued,
	 * @param firstIdToken
	 * @param secondIdToken
	 */
	private void checkIssuedAt(JsonObject firstIdToken, JsonObject secondIdToken, JsonObject valuesForLog) {
		if (!firstIdToken.has(CLAIM_IAT)) {
			throw error("Initial id token does not contain an "+CLAIM_IAT+" claim", args("claimName", CLAIM_IAT));
		}
		if (!secondIdToken.has(CLAIM_IAT)) {
			throw error("Second id token does not contain an "+CLAIM_IAT+" claim", args("claimName", CLAIM_IAT));
		}
		JsonPrimitive claim1 = firstIdToken.getAsJsonPrimitive(CLAIM_IAT);
		JsonPrimitive claim2 = secondIdToken.getAsJsonPrimitive(CLAIM_IAT);
		if(claim1.equals(claim2)) {
			throw error("iat for the second id token MUST represent the time that the new ID Token is issued, " +
					"cannot be the same as the initial id token",
				args("First iat", claim1, "Second iat", claim2));
		}
		JsonObject values = new JsonObject();
		values.addProperty("first", claim1.getAsNumber());
		values.addProperty("second", claim2.getAsNumber());
		values.addProperty("note", "Values are expected to be different");
		valuesForLog.add(CLAIM_IAT, values);
	}

	private void ensureClaimsExistAndAreEqual(JsonObject firstIdToken, JsonObject secondIdToken, String claimName, JsonObject valuesForLog) {
		if (!firstIdToken.has(claimName)) {
			throw error("Initial id token does not contain a "+claimName+" claim", args("claimName", claimName));
		}
		if (!secondIdToken.has(claimName)) {
			throw error("Second id token does not contain a "+claimName+" claim", args("claimName", claimName));
		}
		JsonPrimitive claim1 = firstIdToken.getAsJsonPrimitive(claimName);
		JsonPrimitive claim2 = secondIdToken.getAsJsonPrimitive(claimName);
		if(!claim1.equals(claim2)) {
			throw error("Claim values are not the same", args("claim1", claim1, "claim2", claim2));
		}
		JsonObject values = new JsonObject();
		values.addProperty("first", claim1.getAsString());
		values.addProperty("second", claim2.getAsString());
		values.addProperty("note", "Values are expected to be equal");
		valuesForLog.add(claimName, values);
	}
}
