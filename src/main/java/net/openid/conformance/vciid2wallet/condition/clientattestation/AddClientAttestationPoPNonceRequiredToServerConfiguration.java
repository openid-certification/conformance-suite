package net.openid.conformance.vciid2wallet.condition.clientattestation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddClientAttestationPoPNonceRequiredToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = {"server"})
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		boolean supported = false;
		server.addProperty("client_attestation_pop_nonce_required", supported);

		log("Added " + supported + " to client_attestation_pop_nonce_required in server metadata", args("server", server));


		return env;
	}
}
