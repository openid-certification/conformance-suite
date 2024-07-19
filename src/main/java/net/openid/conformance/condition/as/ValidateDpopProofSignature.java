package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractVerifyJwsSignature;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class ValidateDpopProofSignature extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = { "incoming_dpop_proof", "incoming_request" })
	public Environment evaluate(Environment env) {

		String dpopProofString = env.getString("incoming_dpop_proof", "value");
		JsonElement jwkJson = env.getElementFromObject("incoming_dpop_proof", "header.jwk");
		try {
			JWK jwk = JWK.parse(jwkJson.toString());
			JWKSet jwkSet = new JWKSet(jwk);
			JsonObject jwkSetObj = (JsonObject) JsonParser.parseString(jwkSet.toString());
			verifyJwsSignature(dpopProofString, jwkSetObj, "DPoP Proof", false, "dpop proof header");
		}
		catch (ParseException e) {
			throw error("Invalid DPoP Proof jwk", args("jwk", jwkJson.toString()));
		}
		return env;
	}
}
