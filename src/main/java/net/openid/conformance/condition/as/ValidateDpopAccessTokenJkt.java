package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class ValidateDpopAccessTokenJkt extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"dpop_access_token", "incoming_dpop_proof"})
	public Environment evaluate(Environment env) {

		// Compare the stored jkt thumbprint with the DPoP Proof JWK
		JsonElement jsonJwk = env.getElementFromObject("incoming_dpop_proof", "header.jwk");
		if (jsonJwk == null) {
			throw error("'jwk' claim in DPoP Proof is missing");
		}
		try {
			JWK jwk = JWK.parse(jsonJwk.toString());
			String dpopAccessToken = env.getString("dpop_access_token", "value");
			String jkt = env.getString("dpop_access_token", "jkt");
			if(Strings.isNullOrEmpty(jkt)) {
				throw error("DPoP Access Token jkt binding is not available", args("DPoP Access Token", env.getObject("dpop_access_token")));
			}

			String computedJkt = jwk.computeThumbprint().toString();
			if(!computedJkt.equals(jkt)) {
				throw error("DPoP Access Token jkt does not match JWK thumbprint", args("expected", jkt, "actual", computedJkt));
			}
			logSuccess("DPoP Access Token is constrained to DPoP Proof JWK", args("DPoP Access Token", dpopAccessToken));
			return env;
		}
		catch(ParseException | JOSEException e) {
			throw error("Invalid DPoP Proof jwk", args("jwk", jsonJwk.toString()));
		}
	}

}
