package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractEnsureJwksDoesNotContainPrivateOrSymmetricKeys extends AbstractCondition {

	protected Environment verifyJwksDoesNotContainPrivateOrSymmetricKeys(Environment env, JsonObject jwks) {
		List<JsonElement> privateKeys = new LinkedList<>();
		List<JsonElement> symmetricKeys = new LinkedList<>();
		try {
			JWKSet jwkSet = JWKSet.parse(jwks.toString());
			for(JWK jwk : jwkSet.getKeys()) {
				if(jwk instanceof OctetSequenceKey) {
					//OctetSequenceKey.isPrivate() always returns true
					symmetricKeys.add(new JsonParser().parse(jwk.toString()));
				} else if(jwk.isPrivate()) {
					privateKeys.add(new JsonParser().parse(jwk.toString()));
				}
			}
		} catch (ParseException e) {
			throw error("Invalid jwks", args("jwks", jwks));
		}
		if(privateKeys.isEmpty() && symmetricKeys.isEmpty()) {
			logSuccess("Jwks does not contain any private or symmetric keys");
			return env;
		}
		throw error("Jwks contains private and/or symmetric keys",
					args("private_keys", privateKeys, "symmetric_keys", symmetricKeys));
	}

}
