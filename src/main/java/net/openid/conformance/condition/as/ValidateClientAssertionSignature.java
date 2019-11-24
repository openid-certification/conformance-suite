package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.proc.SimpleSecurityContext;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.ValidateIdTokenSignature;
import net.openid.conformance.testmodule.Environment;

import java.security.Key;
import java.text.ParseException;
import java.util.List;

public class ValidateClientAssertionSignature extends ValidateIdTokenSignature {

	@Override
	@PreEnvironment(required = { "client", "client_assertion" })
	public Environment evaluate(Environment env) {

		String clientAssertionString = env.getString("client_assertion", "value");
		JsonObject client = env.getObject("client");
		JsonObject clientJWKS = client.get("jwks").getAsJsonObject();
		validateTokenSignature(clientAssertionString, clientJWKS, "client_assertion");

		return env;
	}
}
