package net.openid.conformance.condition.client;

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

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AddMultipleRedirectUriToDynamicRegistrationRequest_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private static JsonObject dynamicRegistrationRequest = new JsonParser().parse("{"
		+ "\"client_name\":\"UNIT-TEST client\","
		+ "\"grant_types\":[\"implicit\"]"
		+ "}").getAsJsonObject();

	private static JsonObject goodDynamicRegistrationRequest = new JsonParser().parse("{"
		+ "\"grant_types\":[\"implicit\"],"
		+ "\"client_name\":\"UNIT-TEST client\","
		+ "\"redirect_uris\":[\"https://example1.org/redirect\", \"https://example.org/redirect\"]"
		+ "}").getAsJsonObject();

	private AddMultipleRedirectUriToDynamicRegistrationRequest cond;

	@Before
	public void setUp(){
		cond = new AddMultipleRedirectUriToDynamicRegistrationRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noErrors(){
		env.putString("redirect_uri","https://example1.org/redirect");
		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);
		cond.execute(env);
		assertThat(env.getObject("dynamic_registration_request").equals(goodDynamicRegistrationRequest)).isTrue();
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingDynamicRegistrationRequest(){
		env.putString("redirect_uri","https://example1.org/redirect");
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingRedirectUriInEnvironment(){
		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);
		cond.execute(env);
	}
}
