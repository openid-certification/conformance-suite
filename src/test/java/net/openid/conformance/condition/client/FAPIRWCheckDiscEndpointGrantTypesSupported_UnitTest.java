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
public class FAPIRWCheckDiscEndpointGrantTypesSupported_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private FAPIRWCheckDiscEndpointGrantTypesSupported cond;

	@Before
	public void setUp() throws Exception {
		cond = new FAPIRWCheckDiscEndpointGrantTypesSupported();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {
		JsonObject server = new JsonParser().parse("{"
			+ "\"grant_types_supported\": ["
				+ "\"authorization_code\""
			+ "]}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_missing () {
		JsonObject server = new JsonParser().parse("{"
			+ "}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_error () {
		JsonObject server = new JsonParser().parse("{"
			+ "\"grant_types_supported\": ["
			+ "\"authorization_codex\""
			+ "]}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_errorString () {
		JsonObject server = new JsonParser().parse("{"
			+ "\"grant_types_supported\": \"authorization_code\""
			+ "}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

}
