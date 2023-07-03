package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;

/**
 * Compare the claims between two id tokens, at least one of which was issued from the refresh token grant
 *
 * Implements checks as per:
 *
 * https://openid.net/specs/openid-connect-core-1_0.html#RefreshTokenResponse
 */
public class CompareIdTokenClaims extends AbstractCondition {
	private static final String CLAIM_AUTH_TIME = "auth_time";
	private static final String CLAIM_IAT = "iat";
	private static final String CLAIM_AZP = "azp";
	private static final String CLAIM_AUD = "aud";

	@Override
	@PreEnvironment(required = {"first_id_token", "second_id_token"})
	public Environment evaluate(Environment env) {
		JsonObject firstIdToken = env.getObject("first_id_token").getAsJsonObject("claims");
		JsonObject secondIdToken = env.getObject("second_id_token").getAsJsonObject("claims");

		JsonObject valuesForLog = new JsonObject();
		ensureClaimsExistAndAreEqual(firstIdToken, secondIdToken, "iss", valuesForLog);
		ensureClaimsExistAndAreEqual(firstIdToken, secondIdToken, "sub", valuesForLog);
		checkIssuedAt(firstIdToken, secondIdToken, valuesForLog);
		checkAud(firstIdToken, secondIdToken, valuesForLog);
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
		values.addProperty("first", OIDFJSON.getString(claim1));
		values.addProperty("second", OIDFJSON.getString(claim2));
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
		values.addProperty("first", OIDFJSON.getNumber(claim1));
		values.addProperty("second", OIDFJSON.getNumber(claim2));
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
		values.addProperty("first", OIDFJSON.getNumber(claim1));
		values.addProperty("second", OIDFJSON.getNumber(claim2));
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
		values.addProperty("first", OIDFJSON.getString(claim1));
		values.addProperty("second", OIDFJSON.getString(claim2));
		values.addProperty("note", "Values are expected to be equal");
		valuesForLog.add(claimName, values);
	}

	private void checkAud(JsonObject firstIdToken, JsonObject secondIdToken, JsonObject valuesForLog) {
		if (!firstIdToken.has(CLAIM_AUD)) {
			throw error("Initial id token does not contain an "+CLAIM_AUD+" claim", args("claimName", CLAIM_AUD));
		}
		if (!secondIdToken.has("aud")) {
			throw error("Second id token does not contain an "+CLAIM_AUD+" claim", args("claimName", CLAIM_AUD));
		}
		JsonObject values = new JsonObject();

		if(firstIdToken.get(CLAIM_AUD).isJsonArray()) {
			JsonArray claim1 = firstIdToken.getAsJsonArray(CLAIM_AUD);
			JsonArray claim2 = secondIdToken.getAsJsonArray(CLAIM_AUD);
			values.add("first", claim1);
			values.add("second", claim2);

			Set<String> claim1AudSet = new HashSet<>();
			claim1.forEach(e -> claim1AudSet.add(OIDFJSON.getString(e)));

			Set<String> claim2AudSet = new HashSet<>();
			claim2.forEach(e -> claim2AudSet.add(OIDFJSON.getString(e)));

			if (!claim1AudSet.equals(claim2AudSet)) {
				throw error("aud Claim Value MUST be the same as in the ID Token issued when the original authentication occurred",
					args("First aud", claim1, "Second aud", claim2));
			}

		} else {
			JsonPrimitive claim1 = firstIdToken.getAsJsonPrimitive(CLAIM_AUD);
			JsonPrimitive claim2 = secondIdToken.getAsJsonPrimitive(CLAIM_AUD);
			values.addProperty("first", OIDFJSON.getString(claim1));
			values.addProperty("second", OIDFJSON.getString(claim2));
			if (!claim1.equals(claim2)) {
				throw error("aud Claim Value MUST be the same as in the ID Token issued when the original authentication occurred",
					args("First aud", claim1, "Second aud", claim2));
			}
		}

		values.addProperty("note", "Values are expected to be equal");
		valuesForLog.add(CLAIM_AUD, values);
	}
}
