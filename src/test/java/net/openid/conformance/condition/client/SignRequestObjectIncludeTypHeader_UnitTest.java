package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
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

@ExtendWith(MockitoExtension.class)
public class SignRequestObjectIncludeTypHeader_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject jwks;

	private JsonObject requestObjectClaims;

	private SignRequestObjectIncludeTypHeader cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new SignRequestObjectIncludeTypHeader();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		requestObjectClaims = JsonParser.parseString(
			"""
			  {
			   "iss": "s6BhdRkqt3",
			   "aud": "https://server.example.com",
			   "response_type": "code",
			   "client_id": "s6BhdRkqt3",
			   "scope": "openid"
			  }
			""")
			.getAsJsonObject();

		jwks = JsonParser.parseString("{"
			+ "\"keys\":["
			+ "{"
			+ "\"kty\":\"oct\","
			+ "\"alg\":\"HS256\","
			+ "\"k\":\"LAEuRo0oklLIyw/md746i3ZhbCPd4UoZ7+J421/avfM\""
			+ "}"
			+ "]}").getAsJsonObject();

	}

	@Test
	public void testEvaluate_includesTypAndHeaderParams() throws JOSEException, ParseException {

		JsonObject headerParams = new JsonObject();
		headerParams.addProperty("extra_param", "extra_value");
		JsonArray trustChain = new JsonArray();
		trustChain.add("chain1");
		trustChain.add("chain2");
		headerParams.add("trust_chain", trustChain);

		env.putObject("client_jwks", jwks);
		env.putObject("request_object_claims", requestObjectClaims);
		env.putObject("request_object_header", headerParams);

		cond.execute(env);

		String requestObjectString = env.getString("request_object");
		assertThat(requestObjectString).isNotNull();

		SignedJWT jwt = SignedJWT.parse(requestObjectString);

		// Verify typ header
		assertThat(jwt.getHeader().getType().toString()).isEqualTo("oauth-authz-req+jwt");

		// Verify custom header params
		assertThat(jwt.getHeader().getCustomParam("extra_param")).asString().contains("extra_value");

		Object trustChainHeader = jwt.getHeader().getCustomParam("trust_chain");
		assertThat(trustChainHeader).isInstanceOf(List.class);
		@SuppressWarnings("unchecked")
		List<String> trustChainList = (List<String>) trustChainHeader;
		assertThat(trustChainList).containsExactly("chain1", "chain2");

		// Validate the signature
		JWKSet jwkSet = JWKSet.parse(jwks.toString());
		SecurityContext context = new SimpleSecurityContext();
		JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);
		JWSKeySelector<SecurityContext> selector = new JWSVerificationKeySelector<>(jwt.getHeader().getAlgorithm(), jwkSource);

		List<? extends Key> keys = selector.selectJWSKeys(jwt.getHeader(), context);
		boolean validSignature = false;
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

	@Test
	public void testEvaluate_noHeaderParams() throws JOSEException, ParseException {

		env.putObject("client_jwks", jwks);
		env.putObject("request_object_claims", requestObjectClaims);

		cond.execute(env);

		String requestObjectString = env.getString("request_object");
		assertThat(requestObjectString).isNotNull();

		SignedJWT jwt = SignedJWT.parse(requestObjectString);

		// Verify typ header is still there
		assertThat(jwt.getHeader().getType().toString()).isEqualTo("oauth-authz-req+jwt");
	}
}
