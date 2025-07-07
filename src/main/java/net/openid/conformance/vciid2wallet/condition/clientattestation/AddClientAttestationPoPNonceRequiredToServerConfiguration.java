package net.openid.conformance.vciid2wallet.condition.clientattestation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddClientAttestationPoPNonceRequiredToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = {"server", "client_attestation_metadata"})
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		// we use an empty array for now to indicate that no endpoint requires a client attestation nonce.
		JsonArray endpointsSupportingNonce = new JsonArray();

		JsonObject clientAttestationMetadata = new JsonObject();
		clientAttestationMetadata.add("endpoints_supporting_nonce", endpointsSupportingNonce);
		env.putObject("client_attestation_metadata", clientAttestationMetadata);

		server.add("client_attestation_pop_nonce_required", endpointsSupportingNonce);

		log("Added client_attestation_pop_nonce_required to OAuth server metadata", args("server", server, "client_attestation_pop_nonce_required", endpointsSupportingNonce));

		return env;
	}
}
