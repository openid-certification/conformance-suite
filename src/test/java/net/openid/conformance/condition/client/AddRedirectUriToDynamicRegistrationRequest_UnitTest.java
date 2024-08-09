package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class AddRedirectUriToDynamicRegistrationRequest_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private static JsonObject dynamicRegistrationRequest = JsonParser.parseString("{"
		+ "\"client_name\":\"UNIT-TEST client\","
		+ "\"grant_types\":[\"implicit\"]"
		+ "}").getAsJsonObject();

	private static JsonObject goodDynamicRegistrationRequest = JsonParser.parseString("{"
		+ "\"grant_types\":[\"implicit\"],"
		+ "\"client_name\":\"UNIT-TEST client\","
		+ "\"redirect_uris\":[\"https://example.org/redirect\"]"
		+ "}").getAsJsonObject();

	private AddRedirectUriToDynamicRegistrationRequest cond;

	@BeforeEach
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
		cond.execute(env);
		assertThat(env.getObject("dynamic_registration_request").equals(goodDynamicRegistrationRequest)).isTrue();
	}
}
