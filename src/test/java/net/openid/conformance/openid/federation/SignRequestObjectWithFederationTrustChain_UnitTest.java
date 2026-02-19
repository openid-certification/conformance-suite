package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class SignRequestObjectWithFederationTrustChain_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject jwks;

	private JsonObject requestObjectClaims;

	private SignRequestObjectWithFederationTrustChain cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new SignRequestObjectWithFederationTrustChain();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		requestObjectClaims = JsonParser.parseString("{"
								  + " \"iss\": \"s6BhdRkqt3\","
								  + " \"aud\": \"https://server.example.com\","
								  + " \"response_type\": \"code\","
								  + " \"client_id\": \"s6BhdRkqt3\","
								  + " \"scope\": \"openid\""
								  + "}")
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
	public void testEvaluate_noTrustChain() throws Exception {

		env.putObject("client_jwks", jwks);
		env.putObject("request_object_claims", requestObjectClaims);

		cond.execute(env);

		String requestObjectString = env.getString("request_object");
		assertThat(requestObjectString).isNotNull();

		SignedJWT jwt = SignedJWT.parse(requestObjectString);
		assertThat(jwt.getHeader().getCustomParam("trust_chain")).isNull();
	}

	@Test
	public void testEvaluate_withTrustChain() throws Exception {

		JsonArray trustChain = new JsonArray();
		trustChain.add("statement1");
		trustChain.add("statement2");
		requestObjectClaims.add("trust_chain", trustChain);

		env.putObject("client_jwks", jwks);
		env.putObject("request_object_claims", requestObjectClaims);

		cond.execute(env);

		String requestObjectString = env.getString("request_object");
		assertThat(requestObjectString).isNotNull();

		SignedJWT jwt = SignedJWT.parse(requestObjectString);
		Object trustChainHeader = jwt.getHeader().getCustomParam("trust_chain");
		assertThat(trustChainHeader).isInstanceOf(List.class);
		List<String> trustChainList = (List<String>) trustChainHeader;
		assertThat(trustChainList).containsExactly("statement1", "statement2");

		// Verify it's removed from the body
		assertThat(jwt.getJWTClaimsSet().getClaim("trust_chain")).isNull();
	}
}
