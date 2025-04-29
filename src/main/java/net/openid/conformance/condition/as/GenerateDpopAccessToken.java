package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import java.text.ParseException;

public class GenerateDpopAccessToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"incoming_dpop_proof"})
	@PostEnvironment(required = "dpop_access_token", strings = {"access_token","token_type"})
	public Environment evaluate(Environment env) {

		JsonElement jsonJwk = env.getElementFromObject("incoming_dpop_proof", "header.jwk");
		if (jsonJwk == null) {
			throw error("'jwk' claim in DPoP Proof is missing");
		}
		try {
			JWK jwk = JWK.parse(jsonJwk.toString());
			String computedJkt = jwk.computeThumbprint().toString();

			String dpopAccessToken = RandomStringUtils.secure().nextAlphanumeric(50);
			env.putString("dpop_access_token", "value", dpopAccessToken);
			env.putString("dpop_access_token", "jkt", computedJkt);

			// Needed for CreateTokenEndpointResponse
			env.putString("access_token", dpopAccessToken);
			env.putString("token_type", "DPoP");

			logSuccess("Generated DPoP access token and jkt for DPoP Proof JWK", args("dpop_access_token", env.getObject("dpop_access_token")));
			return env;
		} catch (ParseException | JOSEException e) {
			throw error("Invalid DPoP Proof JWK", args("jwk", jsonJwk.toString()));
		}
	}
}
