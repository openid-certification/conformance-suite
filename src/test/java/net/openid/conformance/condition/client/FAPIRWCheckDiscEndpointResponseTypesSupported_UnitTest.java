package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FAPIRWCheckDiscEndpointResponseTypesSupported_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private FAPIRWCheckDiscEndpointResponseTypesSupported cond;

	@Before
	public void setUp() throws Exception {
		cond = new FAPIRWCheckDiscEndpointResponseTypesSupported();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {
		JsonObject server = new JsonParser().parse("{"
			+ "\"response_types_supported\": ["
				+ "\"code\","
				+ "\"code id_token\""
			+ "]}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_noErrorWithReverseResponseTypes () {
		JsonObject server = new JsonParser().parse("{"
			+ "\"response_types_supported\": ["
				+ "\"code\","
				+ "\"id_token code\""
			+ "]}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_errorWithResponseTypes2Spaces () {
		JsonObject server = new JsonParser().parse("{"
			+ "\"response_types_supported\": ["
				+ "\"code\","
				+ "\"code  id_token\""
			+ "]}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_errorWithResponseTypesTabFormat () {
		JsonObject server = new JsonParser().parse("{"
			+ "\"response_types_supported\": ["
				+ "\"code\","
				+ "\"code\tid_token\""
			+ "]}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_errorWithResponseTypesSupportedNull () {
		JsonObject server = new JsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_errorWithResponseTypesSupportedIsNotArray () {
		JsonObject server = new JsonParser().parse("{"
			+ "\"response_types_supported\": \"is not an array\"}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

}
