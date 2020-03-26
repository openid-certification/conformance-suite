package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class VerifyNewJwksHasNewSigningKey extends AbstractCompareJwks {

	@Override
	protected void compareJwks(Set<JsonObject> originalSigningKeys, Set<JsonObject> latestSigningKeys) {
		Set<JsonObject> keysOnlyInNew = new HashSet<>(latestSigningKeys);
		keysOnlyInNew.removeAll(originalSigningKeys);

		if (keysOnlyInNew.size() == 0) {
			throw error("No new keys with 'use':'sig' (or no 'use') found",
				args("original_signing_keys", originalSigningKeys, "latest_signing_keys", latestSigningKeys));
		}

		// for each new key, verify it is actually new
		keysOnlyInNew.forEach(k -> {
			String kid = OIDFJSON.getString(k.getAsJsonPrimitive("kid"));

			Set<JsonObject> keysWithMatchingKids = originalSigningKeys.stream()
				.filter(mk -> OIDFJSON.getString(mk.getAsJsonPrimitive("kid")).equals(kid))
				.collect(Collectors.toSet());

			if (keysWithMatchingKids.size() > 0) {
				throw error("One of the new keys uses the same kid as one of the original keys",
					args("original_signing_keys", originalSigningKeys,
						"latest_signing_keys", latestSigningKeys,
						"bad_kid", kid));
			}

			String kty = OIDFJSON.getString(k.getAsJsonPrimitive("kty"));
			String field;
			switch (kty) {
				case "RSA": field = "n"; break;
				case "EC": field = "x"; break; // it seems sufficient for 'x' to be the same, no need to check 'y'
				default:
					throw error("unknown key type '"+kty+"' found", args("jwk", k));
			}

			String exponent = OIDFJSON.getString(k.getAsJsonPrimitive(field));
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
