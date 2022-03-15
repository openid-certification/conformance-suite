package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.JSONObjectUtils;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.testmodule.OIDFJSON;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class VerifyNewJwksHasNewSigningKey extends AbstractCompareJwks {

	/**
	 * Returns the set of JSON objects that correspond the public key components of the input JSON key objects.
	 * The resulting key components include only the public key components, kty, and kid. All other information
	 * is removed so that key lists can be compared.
	 * The input JWK keys components must be valid components.
	 * @param inputKeys Set of JSON objects representing JWK key components (RSA, EC)
	 * @return Set of JSON objects that contains the public key components including kid, kty
	 * @throws ConditionError error when JWK components are invalid
	 */
	private Set<JsonObject> getPubKeysWithKeyId(Set<JsonObject> inputKeys) throws ConditionError {
		return inputKeys.stream().map(key -> {
			try {
				JWK jwk = JWK.parse(key.toString());
				var requiredParamsJson = (JsonObject) JsonParser.parseString(JSONObjectUtils.toJSONString(jwk.getRequiredParams()));
				requiredParamsJson.addProperty("kid", jwk.getKeyID());
				return requiredParamsJson;
			} catch (ParseException  e) {
				throw error("Error parsing JWK key", e, args("key", key));
			}
		}).collect(Collectors.toSet());
	}

	@Override
	protected void compareJwks(Set<JsonObject> originalSigningKeys, Set<JsonObject> latestSigningKeys) {
		Set<JsonObject> origSigningPubKeys = getPubKeysWithKeyId(originalSigningKeys);
		Set<JsonObject> latestSigningPubKeys = getPubKeysWithKeyId(latestSigningKeys);
		Set<JsonObject> keysOnlyInNew = new HashSet<>(latestSigningPubKeys);
		keysOnlyInNew.removeAll(origSigningPubKeys);
		if (keysOnlyInNew.isEmpty()) {
			throw error("No new keys with 'use':'sig' (or no 'use') found",
				args("original_signing_keys", originalSigningKeys, "latest_signing_keys", latestSigningKeys));
		}

		// for each new key, verify it is actually new
		keysOnlyInNew.forEach(newKeyToCheck -> {
			String kid = OIDFJSON.getString(newKeyToCheck.getAsJsonPrimitive("kid"));

			Set<JsonObject> keysWithMatchingKids = originalSigningKeys.stream()
				.filter(mk -> OIDFJSON.getString(mk.getAsJsonPrimitive("kid")).equals(kid))
				.collect(Collectors.toSet());

			if (keysWithMatchingKids.size() > 0) {
				throw error("One of the new keys uses the same kid as one of the original keys",
					args("original_signing_keys", originalSigningKeys,
						"latest_signing_keys", latestSigningKeys,
						"bad_kid", kid));
			}

			String kty = OIDFJSON.getString(newKeyToCheck.getAsJsonPrimitive("kty"));
			String field;
			switch (kty) {
				case "RSA": field = "n"; break;
				case "EC": field = "x"; break; // it seems sufficient for 'x' to be the same, no need to check 'y'
				default:
					throw error("unknown key type '"+kty+"' found", args("jwk", newKeyToCheck));
			}

			String exponent = OIDFJSON.getString(newKeyToCheck.getAsJsonPrimitive(field));
			Set<JsonObject> keysWithSameExponent = originalSigningKeys.stream()
				.filter(mk -> mk.getAsJsonPrimitive(field) != null && OIDFJSON.getString(mk.getAsJsonPrimitive(field)).equals(exponent))
				.collect(Collectors.toSet());

			if (keysWithSameExponent.size() > 0) {
				throw error("One of the new keys uses the same exponent as one of the original keys",
					args("original_signing_keys", originalSigningKeys,
						"latest_signing_keys", latestSigningKeys,
						"bad_"+field, exponent));
			}
		});

		logSuccess("Found new keys", args("new_signing_keys", keysOnlyInNew));
	}
}
