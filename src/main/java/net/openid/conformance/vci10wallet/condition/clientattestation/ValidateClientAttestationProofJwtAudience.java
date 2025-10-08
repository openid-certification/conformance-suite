package net.openid.conformance.vci10wallet.condition.clientattestation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateClientAttestationProofJwtAudience extends AbstractCondition {

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

		JsonElement clientAttestationPopAudEl = env.getElementFromObject("client_attestation_pop_object", "claims.aud");
		if (clientAttestationPopAudEl == null) {
			throw error("aud claim missing on client_attestation_pop_object", args("client_attestation_pop_object", clientAttestationPopObj));
		}

		// ensure that the presented aud element contains the authorization server as issuer, not the credential issuer.
		String audValue = OIDFJSON.getString(clientAttestationPopAudEl);
		String authorizationServerIssuer = env.getString("server", "issuer");
		if (!audValue.equals(authorizationServerIssuer)) {
			throw error("aud claim in client_attestation_pop_object did not match the authorization server issuer", args("client_attestation_pop_object", clientAttestationPopObj));
		}

		logSuccess("Found valid aud value matching the authorization server issuer in client attestation proof jwt for jti=" + jti, args("issuer", audValue));

		return env;
	}
}
