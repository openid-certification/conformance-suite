package net.openid.conformance.condition.common;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CheckCIBAServerConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject goodConfig;

	private JsonObject badConfigWithoutAuthorizationEndpoint;

	private JsonObject badConfigWithoutTokenEndpoint;

	private JsonObject badConfigWithoutIssuer;

	private CheckCIBAServerConfiguration cond;

	/**
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CheckCIBAServerConfiguration();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodConfig = new JsonParser().parse("{"
			+ "\"backchannel_authentication_endpoint\":\"https://example.com/oauth/backauth\","
			+ "\"token_endpoint\":\"https://example.com/api/oauth/token\","
			+ "\"issuer\":\"https://example.com/\""
			+ "}").getAsJsonObject();

		badConfigWithoutAuthorizationEndpoint = new JsonParser().parse("{"
			+ "\"token_endpoint\":\"https://example.com/api/oauth/token\","
			+ "\"issuer\":\"https://example.com/\""
			+ "}").getAsJsonObject();

		badConfigWithoutTokenEndpoint = new JsonParser().parse("{"
			+ "\"backchannel_authentication_endpoint\":\"https://example.com/oauth/backauth\","
			+ "\"issuer\":\"https://example.com/\""
			+ "}").getAsJsonObject();

		badConfigWithoutIssuer = new JsonParser().parse("{"
			+ "\"backchannel_authentication_endpoint\":\"https://example.com/oauth/authorize\","
			+ "\"token_endpoint\":\"https://example.com/api/oauth/token\""
			+ "}").getAsJsonObject();
	}

	@Test
	public void testEvaluate_noError() {

		env.putObject("server", goodConfig);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("server", "backchannel_authentication_endpoint");
		verify(env, atLeastOnce()).getString("server", "token_endpoint");
		verify(env, atLeastOnce()).getString("server", "issuer");
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingAuthorizationEndpoint() {

		env.putObject("server", badConfigWithoutAuthorizationEndpoint);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingTokenEndpoint() {

		env.putObject("server", badConfigWithoutTokenEndpoint);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIssuer() {

		env.putObject("server", badConfigWithoutIssuer);

		cond.execute(env);
	}

}
