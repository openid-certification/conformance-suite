package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class SetDpopRefreshTokenCnfJkt extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"dpop_refresh_token_claims", "incoming_dpop_proof"})
	public Environment evaluate(Environment env) {

		JsonElement jsonJwk = env.getElementFromObject("incoming_dpop_proof", "header.jwk");
		if (jsonJwk == null) {
			throw error("'jwk' claim in DPoP Proof is missing");
		}
		try {
			JWK jwk = JWK.parse(jsonJwk.toString());
			String computedJkt = jwk.computeThumbprint().toString();
			env.putString("dpop_refresh_token_claims", "cnf.jkt", computedJkt);
			logSuccess("DPoP Refresh Token is constrained to DPoP Proof JWK", args("DPoP Refresh Token cnf['jkt']", computedJkt, "JWK", jsonJwk));
			return env;
		}
		catch(ParseException | JOSEException e) {
			throw error("Invalid DPoP Proof JWK", args("jwk", jsonJwk.toString()));
		}
	}
}
