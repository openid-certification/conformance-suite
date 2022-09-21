package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class AddIssAsDidJwkForProofKeyToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		JsonObject jwk = (JsonObject)env.getElementFromObject("client", "proof_jwk");
		JWK signingJwk;
		try {
			signingJwk = JWK.parse(jwk.toString());
		} catch (ParseException e) {
			throw new RuntimeException(e); // FIXME
		}
		String pubKey = signingJwk.toPublicJWK().toJSONString();

		Base64URL base64key = Base64URL.encode(pubKey);

		requestObjectClaims.addProperty("iss", "did:jwk:"+base64key.toString());

		logSuccess("Added iss to request object claims", args("iss", base64key.toString()));

		return env;
	}
}
