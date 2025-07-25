package net.openid.conformance.vciid2wallet.condition.clientattestation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CheckForClientAttestationProofJwtReuse extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject clientAttestationPopObj = env.getObject("client_attestation_pop_object");
		if (clientAttestationPopObj == null) {
			throw error("missing client_attestation_pop_object");
		}

		JsonElement clientAttestationPopJtiEl = env.getElementFromObject("client_attestation_pop_object", "claims.jti");
		if (clientAttestationPopJtiEl == null) {
			throw error("jti claim missing on client_attestation_pop_object", args("client_attestation_pop_object", clientAttestationPopObj));
		}
		String jti = OIDFJSON.getString(clientAttestationPopJtiEl);

		String key = "client_attestation_pop_jti_" + jti;
		if (env.getString(key) != null) {
			throw error("Detected reuse of client attestation proof jwt for jti=" + jti, args("client_attestation_pop_object", clientAttestationPopObj));
		}

		env.putString(key, jti);
		logSuccess("No reuse found for client attestation proof jwt for jti=" + jti);

		return env;
	}
}
