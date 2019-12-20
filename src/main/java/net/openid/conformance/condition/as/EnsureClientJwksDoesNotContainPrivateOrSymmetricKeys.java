package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

public class EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {

		JsonObject client = env.getObject("client");
		List<JWK> privateKeys = new LinkedList<>();
		List<JWK> symmetricKeys = new LinkedList<>();
		try {
			JsonObject jwks = client.getAsJsonObject("jwks");
			if(jwks==null) {
				throw error("Client does not contain a jwks element", args("client", client));
			}
			JWKSet jwkSet = JWKSet.parse(jwks.toString());
			for(JWK jwk : jwkSet.getKeys()) {
				if(jwk instanceof OctetSequenceKey) {
					//OctetSequenceKey.isPrivate() always returns true
					symmetricKeys.add(jwk);
				} else if(jwk.isPrivate()) {
					privateKeys.add(jwk);
				}
			}
		} catch (ParseException e) {
			throw error("Invalid client jwks", args("client", client));
		}
		if(privateKeys.isEmpty() && symmetricKeys.isEmpty()) {
			logSuccess("Client jwks does not any contain private or symmetric keys");
			return env;
		}
		throw error("Client jwks contains private and/or symmetric keys",
					args("private_keys", privateKeys, "symmetric_keys", symmetricKeys));
	}

}
