package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AddRedirectUriToDynamicRegistrationRequest_UnitTest {
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
		+ "\"redirect_uris\":[\"https://example.org/redirect\"]"
		+ "}").getAsJsonObject();

	private AddRedirectUriToDynamicRegistrationRequest cond;

	@Before
	public void setUp(){
		cond = new AddRedirectUriToDynamicRegistrationRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}


	/**
	 * Test for {@link AddRedirectUriToDynamicRegistrationRequest#evaluate(Environment)}
	 */
	@Test
	public void testEvaluate_noErrors(){
		env.putString("redirect_uri","https://example.org/redirect");
		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);
		cond.evaluate(env);
		assertThat(env.getObject("dynamic_registration_request").equals(goodDynamicRegistrationRequest)).isTrue();
	}
}
