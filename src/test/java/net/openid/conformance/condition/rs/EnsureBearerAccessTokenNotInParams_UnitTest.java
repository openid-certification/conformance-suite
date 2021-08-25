package net.openid.conformance.condition.rs;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
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
	@Before
	public void setUp() throws Exception {

		cond = new EnsureBearerAccessTokenNotInParams();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		hasToken = new JsonParser().parse(
			"{\"query_string_params\": " +
				"{\"access_token\": \"foo123456\"}" +
			"}").getAsJsonObject();
		missingToken = new JsonParser().parse(
			"{\"query_string_params\": " +
				"{}" +
			"}").getAsJsonObject();
		missingParams = new JsonParser().parse(
			"{}").getAsJsonObject();

	}

	/**
	 * Test method for {@link EnsureClientCertificateCNMatchesClientId#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_hasToken() {

		env.putObject("incoming_request", hasToken);

		cond.execute(env);


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
