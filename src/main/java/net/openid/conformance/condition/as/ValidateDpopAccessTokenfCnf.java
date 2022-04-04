package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class ValidateDpopAccessTokenfCnf extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"incoming_dpop_access_token", "incoming_dpop_proof"})
	public Environment evaluate(Environment env) {

		String dpopAccessToken = env.getString("dpop_access_token");

		// Compare the incoming "cnf":{"jkt":"..."} thumbprint with the DPoP Proof JWK
		JsonElement jsonJwk = env.getElementFromObject("incoming_dpop_proof", "header.jwk");
		if (jsonJwk == null) {
			throw error("'jwk' claim in DPoP Proof is missing");
		}

		try {
			JWK jwk = JWK.parse(jsonJwk.toString());
			String jkt = env.getString("incoming_dpop_access_token", "claims.cnf.jkt");
			if(Strings.isNullOrEmpty(jkt)) {
				throw error("cnf['jkt'] claim in DPoP Access Token not available", args("DPoP Access Token", env.getObject("incoming_dpop_proof")));
			}
			String computedJkt = jwk.computeThumbprint().toString();
			if(!computedJkt.equals(jkt)) {
				throw error("Invalid cnf[jkt] in DPoP Access Token", args("expected", computedJkt, "actual", jkt));
			}
			logSuccess("DPoP Access Token is constrained to DPoP Proof JWK", args("DPoP Access Token", dpopAccessToken));
			return env;
		}
		catch(ParseException | JOSEException e) {
			throw error("Invalid DPoP Proof jwk", args("jwk", jsonJwk.toString()));
		}
	}

}
