package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import net.openid.conformance.condition.AbstractCondition;

import java.text.ParseException;

public abstract class AbstractGetSigningKey extends AbstractCondition {
	JWK getSigningKey(String name, JsonObject jwks) {
		int count = 0;
		JWK signingJwk = null;

		JWKSet jwkSet = null;
		try {
			jwkSet = JWKSet.parse(jwks.toString());
		} catch (ParseException e) {
			throw error("Failed to parse " + name + " jwks", e);
		}

		for (JWK jwk : jwkSet.getKeys()) {
			var use = jwk.getKeyUse();
			if (use != null && !use.equals(KeyUse.SIGNATURE)) {
				// skip any encryption keys
				continue;
			}
			count++;
			signingJwk = jwk;
		}

		if (count == 0) {
			throw error("Did not find a key with 'use': 'sig' or no 'use' claim, no key available to sign jwt", args("jwks", jwks));
		}
		if (count > 1) {
			throw error("Expected only one signing JWK in the set. Please ensure the signing key is the only one in the jwks, or that other keys have a 'use' other than 'sig'.", args("jwks", jwks));
		}

		return signingJwk;
	}
}
