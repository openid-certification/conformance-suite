package io.fintechlabs.testframework.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.Key;
import java.text.ParseException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.proc.SimpleSecurityContext;
import com.nimbusds.jwt.SignedJWT;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.SignRequestObject;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class SignRequestObject_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject jwks;

	private JsonObject requestObjectClaims;

	private SignRequestObject cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new SignRequestObject("UNIT-TEST", eventLog, ConditionResult.INFO);

		// Sample values from OpenID Connect Core 1.0 § 6.1

		requestObjectClaims = new JsonParser().parse(
				"  {\n" + 
				"   \"iss\": \"s6BhdRkqt3\",\n" + 
				"   \"aud\": \"https://server.example.com\",\n" + 
				"   \"response_type\": \"code id_token\",\n" + 
				"   \"client_id\": \"s6BhdRkqt3\",\n" + 
				"   \"redirect_uri\": \"https://client.example.org/cb\",\n" + 
				"   \"scope\": \"openid\",\n" + 
				"   \"state\": \"af0ifjsldkj\",\n" + 
				"   \"nonce\": \"n-0S6_WzA2Mj\",\n" + 
				"   \"max_age\": 86400,\n" + 
				"   \"claims\":\n" + 
				"    {\n" + 
				"     \"userinfo\":\n" + 
				"      {\n" + 
				"       \"given_name\": {\"essential\": true},\n" + 
				"       \"nickname\": null,\n" + 
				"       \"email\": {\"essential\": true},\n" + 
				"       \"email_verified\": {\"essential\": true},\n" + 
				"       \"picture\": null\n" + 
				"      },\n" + 
				"     \"id_token\":\n" + 
				"      {\n" + 
				"       \"gender\": null,\n" + 
				"       \"birthdate\": {\"essential\": true},\n" + 
				"       \"acr\": {\"values\": [\"urn:mace:incommon:iap:silver\"]}\n" + 
				"      }\n" + 
				"    }\n" + 
				"  }").getAsJsonObject();

		// Generated key (since the sample in the OpenID spec does not include the private key)

		jwks = new JsonParser().parse("{"
				+ "\"keys\":["
				+ "{"
				+ "\"kty\":\"oct\","
				+ "\"alg\":\"HS256\","
				+ "\"k\":\"LAEuRo0oklLIyw/md746i3ZhbCPd4UoZ7+J421/avfM\""
				+ "}"
				+ "]}").getAsJsonObject();

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.SignRequestObject#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 * @throws JOSEException, ParseException
	 */
	@Test
	public void testEvaluate_valuesPresent() throws JOSEException, ParseException {

		env.put("jwks", jwks);
		env.put("request_object_claims", requestObjectClaims);

		cond.evaluate(env);

		verify(env, atLeastOnce()).get("request_object_claims");

		String requestObjectString = env.getString("request_object");
		assertThat(requestObjectString).isNotNull();

		// Validate the signed object

		boolean validSignature = false;

		SignedJWT jwt = SignedJWT.parse(requestObjectString);
		JWKSet jwkSet = JWKSet.parse(jwks.toString());

		SecurityContext context = new SimpleSecurityContext();

		JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

		JWSKeySelector<SecurityContext> selector = new JWSVerificationKeySelector<>(jwt.getHeader().getAlgorithm(), jwkSource);

		List<? extends Key> keys = selector.selectJWSKeys(jwt.getHeader(), context);
		for (Key key : keys) {
			JWSVerifierFactory factory = new DefaultJWSVerifierFactory();
			JWSVerifier verifier = factory.createJWSVerifier(jwt.getHeader(), key);

			if (jwt.verify(verifier)) {
				validSignature = true;
				break;
			}
		}

		assertThat(validSignature).isTrue();

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.SignRequestObject#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_jwksMissing() {

		env.put("request_object_claims", requestObjectClaims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.SignRequestObject#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_claimsMissing() {

		env.put("jwks", jwks);

		cond.evaluate(env);

	}

}
