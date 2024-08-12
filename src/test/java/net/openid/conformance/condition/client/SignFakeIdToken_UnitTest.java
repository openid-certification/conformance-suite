package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.ParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SignFakeIdToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject jwks;

	private JsonObject idTokenClaims;

	private SignFakeIdToken cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new SignFakeIdToken();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);


		idTokenClaims = JsonParser.parseString(
						"""
								  {
								   "foo": "bar"
								  }\
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
	public void testEvaluate_keyAvailable() throws JOSEException, ParseException {

		env.putObject("client_jwks", jwks);
		env.putObject("id_token_claims", idTokenClaims);
		env.putObject("id_token", new JsonObject());

		cond.execute(env);

		verify(env, atLeastOnce()).getObject("id_token_claims");

		String idTokenString = env.getString("id_token", "value");
		assertThat(idTokenString).isNotNull();

	}

}
