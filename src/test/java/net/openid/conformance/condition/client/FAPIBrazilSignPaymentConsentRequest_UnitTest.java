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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class FAPIBrazilSignPaymentConsentRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject jwks;

	private final String AUD_VALUE = "https://example.com/open-banking/payments/v4/consents";
	private JsonObject consentRequestClaimsAudArray;
	private JsonObject consentRequestClaimsAudString;

	private FAPIBrazilSignPaymentConsentRequest cond;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		cond = new FAPIBrazilSignPaymentConsentRequest();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		// aud claim as single element array.
		consentRequestClaimsAudArray = JsonParser.parseString(String.format(
		"""
		  {
			 "aud" : [ "%s" ]
		  }
		""", AUD_VALUE)).getAsJsonObject();

		// aud claim as string.
		consentRequestClaimsAudString = JsonParser.parseString(String.format(
		"""
		  {
			 "aud" : "%s"
		  }
		""", AUD_VALUE)).getAsJsonObject();

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
	 * Test method for {@link FAPIBrazilSignPaymentConsentRequest#evaluate(Environment)}.
	 *
	 * @throws JOSEException,
	 *	     ParseException
	 */
	@Test
	public void testEvaluate_valuesPresent() throws JOSEException, ParseException {
		env.putObject("client", "org_jwks", jwks);
		env.putObject("consent_endpoint_request", consentRequestClaimsAudString);

		cond.execute(env);

		verify(env, atLeastOnce()).getObject("consent_endpoint_request");

		String consentObjectString = env.getString("consent_endpoint_request_signed");
		assertThat(consentObjectString).isNotNull();

		// Validate the signed object

		boolean validSignature = false;

		SignedJWT jwt = SignedJWT.parse(consentObjectString);
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
	 * Test method for {@link FAPIBrazilSignPaymentConsentRequest#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_jwksMissing() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("request_object_claims", consentRequestClaimsAudString);
			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link FAPIBrazilSignPaymentConsentRequest#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_claimsMissing() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("client", "org_jwks", jwks);
			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link FAPIBrazilSignPaymentConsentRequest#evaluate(Environment)}.
	 *
	 * @throws JOSEException,
	 *	     ParseException
	 */
	@Test
	public void testEvaluate_audAsString() throws JOSEException, ParseException {
		env.putObject("client", "org_jwks", jwks);
		env.putObject("consent_endpoint_request", consentRequestClaimsAudString);

		cond.execute(env);

		verify(env, atLeastOnce()).getObject("consent_endpoint_request");

		String consentObjectString = env.getString("consent_endpoint_request_signed");
		assertThat(consentObjectString).isNotNull();

		// Obtain the 'aid' claim from the signed object.
		SignedJWT jwt = SignedJWT.parse(consentObjectString);
		Object audClaim = jwt.getPayload().toJSONObject().get("aud");

		// Verify the 'aud' claim is a string.
		assertThat(audClaim instanceof String).isTrue();

		// Verify the 'aud' claim value is as expected.
		assertThat(audClaim.equals(AUD_VALUE)).isTrue();
	}

	/**
	 * Test method for {@link FAPIBrazilSignPaymentConsentRequest#evaluate(Environment)}.
	 *
	 * @throws JOSEException,
	 *	     ParseException
	 */
	@Test
	public void testEvaluate_audAsArray() throws JOSEException, ParseException {
		env.putObject("client", "org_jwks", jwks);
		env.putObject("consent_endpoint_request", consentRequestClaimsAudArray);

		cond.execute(env);

		verify(env, atLeastOnce()).getObject("consent_endpoint_request");

		String consentObjectString = env.getString("consent_endpoint_request_signed");
		assertThat(consentObjectString).isNotNull();

		// Obtain the 'aid' claim from the signed object.
		SignedJWT jwt = SignedJWT.parse(consentObjectString);
		Object audClaim = jwt.getPayload().toJSONObject().get("aud");

		// Verify the 'aud' claim is a an array.
		assertThat(audClaim instanceof ArrayList).isTrue();

		// Verify the 'aud' array has a single entry.
		assertThat(((ArrayList)audClaim).size() == 1).isTrue();

		// Verify the 'aud' claim value is as expected.
		assertThat(((ArrayList)audClaim).get(0).equals(AUD_VALUE)).isTrue();
	}
}
