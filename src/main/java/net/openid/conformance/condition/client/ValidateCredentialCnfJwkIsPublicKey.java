package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class ValidateCredentialCnfJwkIsPublicKey extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "sdjwt" } )
	public Environment evaluate(Environment env) {
		JsonElement jwkEl = env.getElementFromObject("sdjwt", "credential.claims.cnf.jwk");
		if (jwkEl == null) {
			throw error("cnf claim in SD-JWT does not include a jwk element");
		}

		try {
			JWK jwk = JWK.parse(jwkEl.toString());
			if(jwk instanceof OctetSequenceKey) {
				//OctetSequenceKey.isPrivate() always returns true
				throw error("cnf.jwk in the SD-JWT credential is a symmetric key", args("jwk", jwkEl));
			} else if(jwk.isPrivate()) {
				throw error("cnf.jwk in the SD-JWT credential is a private key", args("jwk", jwkEl));
			}
		} catch (ParseException e) {
			throw error("Invalid jwk", args("jwk", jwkEl));
		}

		logSuccess("cnf.jwk in the SD-JWT credential is a public key", args("jwk", jwkEl));

		return env;
	}

}
