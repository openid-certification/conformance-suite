package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(MockitoExtension.class)
public class SignKSAConsentRequest_UnitTest {

	@Mock
	private TestInstanceEventLog eventLog;

	private SignKSAConsentRequest cond;
	private Environment env;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new SignKSAConsentRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();

		RSAKey key = new RSAKeyGenerator(2048)
			.keyID("ksa-test-key")
			.algorithm(JWSAlgorithm.PS256)
			.generate();
		JsonObject jwks = new JsonObject();
		jwks.add("keys", JsonParser.parseString("[" + key.toJSONString() + "]"));
		JsonObject client = new JsonObject();
		client.add("jwks", jwks);
		env.putObject("client", client);

		JsonObject claims = new JsonObject();
		claims.addProperty("iss", "client-1234");
		JsonObject message = new JsonObject();
		message.add("Data", new JsonObject());
		claims.add("message", message);
		env.putObject("account_requests_endpoint_request", claims);
	}

	@Test
	public void testProducesSignedJwt() throws Exception {
		env = cond.evaluate(env);

		String jws = env.getString("account_requests_endpoint_request_signed");
		assertThat(jws, notNullValue());
		SignedJWT parsed = SignedJWT.parse(jws);
		assertThat(parsed.getJWTClaimsSet().getIssuer(), is("client-1234"));
		assertThat(parsed.getHeader().getAlgorithm().getName(), is("PS256"));
	}
}
