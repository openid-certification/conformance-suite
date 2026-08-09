package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.RFC6749AppendixASyntaxUtils;
import net.openid.conformance.testmodule.Environment;


public class EnsureDpopNonceContainsAllowedCharactersOnly extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"dpop_proof_claims"})
	public Environment evaluate(Environment env)
	{
		String dpopNonce = env.getString("dpop_proof_claims", "nonce");
		if(dpopNonce==null) {
			log("No DPOP nonce required");
		} else {
			if (!RFC6749AppendixASyntaxUtils.isNQCharSequence(dpopNonce)) {
				throw error("DPOP nonce contains illegal characters. As per RFC-6749, only NQCHAR characters %x21 / %x23-5B / %x5D-7E are allowed.",
					args("DPOP nonce", dpopNonce));
			}
			logSuccess("DPOP nonce does not contain any illegal characters", args("DPOP nonce", dpopNonce));
		}
		return env;
	}
}
