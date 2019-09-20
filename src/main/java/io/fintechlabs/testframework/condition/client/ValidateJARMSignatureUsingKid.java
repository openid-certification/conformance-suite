package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

import java.text.ParseException;

public class ValidateJARMSignatureUsingKid extends ValidateIdTokenSignatureUsingKid {

	@Override
	@PreEnvironment(required = { "jarm_response", "server_jwks" })
	public Environment evaluate(Environment env) {

		String jarmResponse = env.getString("jarm_response", "value");
		JsonObject serverJwks = env.getObject("server_jwks");

		validateTokenSignature(jarmResponse, serverJwks, "jarm_response");

		return env;
	}


}
