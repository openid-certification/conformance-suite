package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
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

		if (!Strings.isNullOrEmpty(dpop)) {
			return extractJWT(env, "incoming_request", "headers.dpop", "incoming_dpop_proof", null, null);
		} else {
			throw error("Couldn't find DPoP Proof header");
		}
	}

}
