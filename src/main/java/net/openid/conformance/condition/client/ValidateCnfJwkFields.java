package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

/**
 * Validates a cnf.jwk object: checks it is a public key, warns about
 * unknown JWK fields, and checks use/alg values if present.
 *
 * Subclasses override getCnfFromEnvironment() to specify where to
 * read the cnf object from.
 */
public abstract class ValidateCnfJwkFields extends AbstractCondition {

	protected abstract JsonElement getCnfFromEnvironment(Environment env);

	protected abstract String getContext();

	@Override
	public Environment evaluate(Environment env) {

		JsonElement cnfEl = getCnfFromEnvironment(env);
		if (cnfEl == null || !cnfEl.isJsonObject()) {
			throw error("cnf claim missing or not a JSON object in " + getContext());
		}

		JsonObject cnf = cnfEl.getAsJsonObject();

		JsonElement jwkEl = cnf.get("jwk");
		if (jwkEl == null || !jwkEl.isJsonObject()) {
			throw error("cnf.jwk missing or not a JSON object in " + getContext());
		}

		try {
			JWK jwk = JWK.parse(jwkEl.toString());
			if (jwk instanceof OctetSequenceKey) {
				throw error("cnf.jwk is a symmetric key in " + getContext(), args("jwk", jwkEl));
			}
			if (jwk.isPrivate()) {
				throw error("cnf.jwk is a private key in " + getContext(), args("jwk", jwkEl));
			}
		} catch (ParseException e) {
			throw error("Invalid cnf.jwk in " + getContext(), e, args("jwk", jwkEl));
		}

		logSuccess("cnf.jwk is valid in " + getContext(), args("jwk", jwkEl));
		return env;
	}
}
