package net.openid.conformance.condition.rs;

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
public class EnsureBearerAccessTokenNotInParams_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureBearerAccessTokenNotInParams cond;

	private JsonObject hasToken;
	private JsonObject missingToken;
	private JsonObject missingParams;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new EnsureBearerAccessTokenNotInParams();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		hasToken = JsonParser.parseString(
			"{\"query_string_params\": " +
				"{\"access_token\": \"foo123456\"}" +
			"}").getAsJsonObject();
		missingToken = JsonParser.parseString(
			"{\"query_string_params\": " +
				"{}" +
			"}").getAsJsonObject();
		missingParams = JsonParser.parseString(
			"{}").getAsJsonObject();

	}

	/**
	 * Test method for {@link EnsureClientCertificateCNMatchesClientId#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_hasToken() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("incoming_request", hasToken);

			cond.execute(env);


		});


	}
	@Test
	public void testEvaluate_missingToken() {

		env.putObject("incoming_request", missingToken);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("incoming_request", "query_string_params.access_token");

	}
	@Test
	public void testEvaluate_missing() {

		env.putObject("incoming_request", missingParams);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("incoming_request", "query_string_params.access_token");

	}
}
