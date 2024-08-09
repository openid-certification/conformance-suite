package net.openid.conformance.condition.client;

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
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Key;
import java.text.ParseException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
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
	@BeforeEach
	public void setUp() throws Exception {

		cond = new SignRequestObject();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		// Sample values from OpenID Connect Core 1.0 § 6.1

		requestObjectClaims = JsonParser.parseString(
						"""
								  {
								   "iss": "s6BhdRkqt3",
								   "aud": "https://server.example.com",
								   "response_type": "code id_token",
								   "client_id": "s6BhdRkqt3",
								   "redirect_uri": "https://client.example.org/cb",
								   "scope": "openid",
								   "state": "af0ifjsldkj",
								   "nonce": "n-0S6_WzA2Mj",
								   "max_age": 86400,
								   "claims":
								    {
								     "userinfo":
								      {
								       "given_name": {"essential": true},
								       "nickname": null,
								       "email": {"essential": true},
								       "email_verified": {"essential": true},
								       "picture": null
								      },
								     "id_token":
								      {
								       "gender": null,
								       "birthdate": {"essential": true},
								       "acr": {"values": ["urn:mace:incommon:iap:silver"]}
								      }
								    }
								  }\
								""")
			.getAsJsonObject();

		// Generated key (since the sample in the OpenID spec does not include the private key)

		jwks = JsonParser.parseString("{"
			+ "\"keys\":["
			+ "{"
			+ "\"kty\":\"oct\","
			+ "\"alg\":\"HS256\","
			+ "\"k\":\"LAEuRo0oklLIyw/md746i3ZhbCPd4UoZ7+J421/avfM\""
			+ "}"
			+ "]}").getAsJsonObject();

	}

	/**
	 * Test method for {@link SignRequestObject#evaluate(Environment)}.
	 *
	 * @throws JOSEException,
	 *             ParseException
	 */
	@Test
	public void testEvaluate_valuesPresent() throws JOSEException, ParseException {

		env.putObject("client_jwks", jwks);
		env.putObject("request_object_claims", requestObjectClaims);

		cond.execute(env);

		verify(env, atLeastOnce()).getObject("request_object_claims");

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
	 * Test method for {@link SignRequestObject#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_jwksMissing() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("request_object_claims", requestObjectClaims);

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link SignRequestObject#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_claimsMissing() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("client_jwks", jwks);

			cond.execute(env);

		});

	}

}
