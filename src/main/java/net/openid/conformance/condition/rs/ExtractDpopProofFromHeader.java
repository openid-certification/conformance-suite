package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractExtractJWT;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractDpopProofFromHeader extends AbstractExtractJWT {

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(required = "incoming_dpop_proof")
	public Environment evaluate(Environment env) {
		String dpop = env.getString("incoming_request", "headers.dpop");
		final String destinationPath = "incoming_dpop_proof";

		env.removeObject(destinationPath); // Remove any existing DPoP information from destinationPath
		if (!Strings.isNullOrEmpty(dpop)) {
			JsonObject privateJwksWithEncKeys = null; // we deliberately don't pass any decryption keys as dpop proofs must not be encrypted
			return extractJWT(env, "incoming_request", "headers.dpop", destinationPath, null, privateJwksWithEncKeys);
		} else {
			throw error("Couldn't find DPoP Proof header");
		}
	}

}
