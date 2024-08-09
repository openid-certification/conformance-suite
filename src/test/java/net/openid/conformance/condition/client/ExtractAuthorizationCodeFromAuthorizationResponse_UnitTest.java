package net.openid.conformance.condition.client;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
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
	@BeforeEach
	public void setUp() throws Exception {

		cond = new ExtractAuthorizationCodeFromAuthorizationResponse();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		callbackParams = JsonParser.parseString("{"
			+ "\"code\":\"SplxlOBeZQQYbYS6WxSbIA\","
			+ "\"state\":\"xyz\""
			+ "}").getAsJsonObject();

		callbackParamsWithoutCode = JsonParser.parseString("{"
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
	@Test
	public void testEvaluate_valueMissing() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("authorization_endpoint_response", callbackParamsWithoutCode);

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link ExtractAuthorizationCodeFromAuthorizationResponse#evaluate(Environment)}.
	 */
	@Test
	public void testparamsMissing() {
		assertThrows(ConditionError.class, () -> {

			cond.execute(env);
		});
	}
}
