package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Set;

public class VerifyNewJwksStillHasOldSigningKey extends AbstractCompareJwks {

	@Override
	protected void compareJwks(Set<JsonObject> originalSigningKeys, Set<JsonObject> latestSigningKeys) {
		Set<JsonObject> keysInBoth = new HashSet<>(originalSigningKeys);
		keysInBoth.retainAll(latestSigningKeys);

		if (keysInBoth.isEmpty()) {
			throw error("None of the previous present keys (with 'use':'sig' or no 'use') are still present. The specification says 'The JWK Set document at the jwks_uri SHOULD retain recently decommissioned signing keys for a reasonable period of time to facilitate a smooth transition.'.",
				args("original_signing_keys", originalSigningKeys, "latest_signing_keys", latestSigningKeys));
		}

		logSuccess("Some keys are in both the old and new JWKS", args("signing_keys_in_both", keysInBoth));
	}
}
