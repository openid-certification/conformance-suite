package net.openid.conformance.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

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
public class ExtractAuthorizationCodeFromAuthorizationResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject callbackParams;

	private JsonObject callbackParamsWithoutCode;

	private ExtractAuthorizationCodeFromAuthorizationResponse cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new ExtractAuthorizationCodeFromAuthorizationResponse();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		callbackParams = new JsonParser().parse("{"
			+ "\"code\":\"SplxlOBeZQQYbYS6WxSbIA\","
			+ "\"state\":\"xyz\""
			+ "}").getAsJsonObject();

		callbackParamsWithoutCode = new JsonParser().parse("{"
			+ "\"state\":\"xyz\""
			+ "}").getAsJsonObject();
	}

	/**
	 * Test method for {@link ExtractAuthorizationCodeFromAuthorizationResponse#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("authorization_endpoint_response", callbackParams);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("authorization_endpoint_response", "code");

		assertThat(env.getString("code")).isEqualTo("SplxlOBeZQQYbYS6WxSbIA");
	}

	/**
	 * Test method for {@link ExtractAuthorizationCodeFromAuthorizationResponse#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		env.putObject("authorization_endpoint_response", callbackParamsWithoutCode);

		cond.execute(env);
	}

	/**
	 * Test method for {@link ExtractAuthorizationCodeFromAuthorizationResponse#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testparamsMissing() {

		cond.execute(env);
	}
}
