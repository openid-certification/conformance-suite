package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ExtractServerSigningAlg_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject jwks;

	private JsonObject invalidJwks;

	private ExtractServerSigningAlg cond;

	private JsonObject combinedJwks;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new ExtractServerSigningAlg();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO, new String[0]);

		jwks = JsonParser.parseString("{"
			+ "\"keys\":["
			+ "{"
			+ "\"kty\":\"oct\","
			+ "\"alg\":\"PS256\","
			+ "\"k\":\"LAEuRo0oklLIyw/md746i3ZhbCPd4UoZ7+J421/avfM\""
			+ "}"
			+ "]}").getAsJsonObject();


		invalidJwks = JsonParser.parseString("{"
			+ "\"keys\":["
			+ "{"
			+ "\"kty\":\"oct\","
			+ "\"k\":\"LAEuRo0oklLIyw/md746i3ZhbCPd4UoZ7+J421/avfM\""
			+ "}"
			+ "]}").getAsJsonObject();

		combinedJwks = JsonParser.parseString("{"
			+ "\"keys\":["
			+ "{"
			+ "\"kty\":\"oct\","
			+ "\"alg\":\"PS256\","
			+ "\"k\":\"LAEuRo0oklLIyw/md746i3ZhbCPd4UoZ7+J421/avfM\""
			+ "}"
			+ ","
			+ "{"
			+ "\"kty\":\"oct\","
			+ "\"alg\":\"PS256\","
			+ "\"k\":\"LAEuRo0oklLIyw/md746i3ZhbCPd4UoZ7+J421/avfM\""
			+ "}"
			+ "]}" ).getAsJsonObject();
	}

	@Test
	public void testEvaluate_goodValues() {

		env.putObject("server_jwks", jwks);

		cond.execute(env);

		verify(env, atLeastOnce()).getObject("server_jwks");

	}

	@Test
	public void testEvaluate_nullAlg(){
		assertThrows(ConditionError.class, () -> {

			env.putObject("server_jwks", invalidJwks);

			cond.execute(env);

			verify(env, atLeastOnce()).getObject("server_jwks");

		});

	}

	@Test
	public void testEvaluate_twoJWK(){
		assertThrows(ConditionError.class, () -> {

			env.putObject("server_jwks", combinedJwks);

			cond.execute(env);

			verify(env, atLeastOnce()).getObject("server_jwks");
		});
	}
}
