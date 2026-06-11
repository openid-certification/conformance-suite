package net.openid.conformance.vci10issuer.condition;

import com.authlete.sd.SDJWT;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

/**
 * Extracts the key binding (cnf.jwk) from each SD-JWT credential issued in a batch and
 * stores them, index-aligned with the credentials, in 'vci_batch_binding_keys' for the
 * batch key binding checks. As the proofs in the credential request demonstrated possession
 * of cryptographic keys, each issued credential must be bound to one of them, so a missing
 * cnf claim is an error.
 */
public class VCIExtractBatchSdJwtBindingKeys extends AbstractCondition {

	@Override
	@PreEnvironment(required = "extracted_credentials")
	@PostEnvironment(required = "vci_batch_binding_keys")
	public Environment evaluate(Environment env) {

		JsonArray list = env.getObject("extracted_credentials").getAsJsonArray("list");

		JsonArray keys = new JsonArray();
		for (int i = 0; i < list.size(); i++) {
			keys.add(extractCnfJwk(OIDFJSON.getString(list.get(i)), i));
		}

		JsonObject bindingKeys = new JsonObject();
		bindingKeys.add("keys", keys);
		env.putObject("vci_batch_binding_keys", bindingKeys);

		logSuccess("Extracted the cnf binding key from each credential in the batch",
			args("binding_keys", keys));

		return env;
	}

	private JsonObject extractCnfJwk(String sdJwtString, int index) {
		JsonObject claims;
		try {
			SDJWT sdJwt = SDJWT.parse(sdJwtString);
			JsonObject credentialJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(sdJwt.getCredentialJwt());
			claims = credentialJwt.getAsJsonObject("claims");
		} catch (IllegalArgumentException | ParseException e) {
			throw error("Parsing SD-JWT failed", e, args("credential_index", index, "credential", sdJwtString));
		}

		JsonElement cnfEl = claims.get("cnf");
		if (cnfEl == null || !cnfEl.isJsonObject()) {
			throw error("Credential issued in response to a credential request containing key proofs does not "
					+ "contain a 'cnf' claim binding it to one of the proof keys",
				args("credential_index", index, "claims", claims));
		}

		JsonElement jwkEl = cnfEl.getAsJsonObject().get("jwk");
		if (jwkEl == null || !jwkEl.isJsonObject()) {
			throw error("'cnf' claim in the issued credential does not contain a 'jwk' key binding",
				args("credential_index", index, "cnf", cnfEl));
		}

		return jwkEl.getAsJsonObject();
	}
}
