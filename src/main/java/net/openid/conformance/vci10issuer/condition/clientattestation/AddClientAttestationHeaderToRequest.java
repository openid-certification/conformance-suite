package net.openid.conformance.vci10issuer.condition.clientattestation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class AddClientAttestationHeaderToRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		if (!env.containsObject("request_headers")) {
			throw error("Couldn't find request headers");
		}

		JsonObject o = env.getObject("request_headers");
		String clientAttestation = env.getString("client_attestation");
		o.addProperty("OAuth-Client-Attestation", clientAttestation);

		// OAuth-Client-Attestation:  A JWT that conforms to the structure and
		//      syntax as defined in Section 5.1
		log("Added OAuth-Client-Attestation header to request", args("client_attestation", clientAttestation));

		return env;
	}
}
