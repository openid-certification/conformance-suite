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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CheckMatchingCallbackParameters_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private String uriWithoutSuffix;

	private String uriWithSuffix;

	private JsonObject goodParams;

	private JsonObject badParams;

	private CheckMatchingCallbackParameters cond;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new CheckMatchingCallbackParameters();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		uriWithoutSuffix = "https://example.com/callback";

		uriWithSuffix = uriWithoutSuffix + "?dummy1=lorem&dummy2=ipsum";

		goodParams = JsonParser.parseString("{"
				+ "\"dummy1\":\"lorem\","
				+ "\"dummy2\":\"ipsum\""
				+ "}").getAsJsonObject();

		badParams = JsonParser.parseString("{"
				+ "\"dummy1\":\"dolor\""
				+ "}").getAsJsonObject();
	}

	/**
	 * Test method for {@link CheckMatchingCallbackParameters#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noSuffix() {

		env.putString("redirect_uri", uriWithoutSuffix);
		env.putObject("callback_query_params", new JsonObject());

		cond.execute(env);

		verify(env, atLeastOnce()).getString("redirect_uri");
	}

	/**
	 * Test method for {@link CheckMatchingCallbackParameters#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_withSuffix_noError() {

		env.putString("redirect_uri", uriWithSuffix);
		env.putObject("callback_query_params", goodParams);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("redirect_uri");
		verify(env, atLeastOnce()).getString("callback_query_params", "dummy1");
		verify(env, atLeastOnce()).getString("callback_query_params", "dummy2");
	}

	/**
	 * Test method for {@link CheckMatchingCallbackParameters#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_withSuffix_badParams() {
		assertThrows(ConditionError.class, () -> {

			env.putString("redirect_uri", uriWithSuffix);
			env.putObject("callback_params", badParams);

			cond.execute(env);
		});
	}

}
