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

@ExtendWith(MockitoExtension.class)
public class AddMultipleRedirectUriToDynamicRegistrationRequest_UnitTest {
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
		+ "\"redirect_uris\":[\"https://example1.org/redirect\", \"https://example.org/redirect\"]"
		+ "}").getAsJsonObject();

	private AddMultipleRedirectUriToDynamicRegistrationRequest cond;

	@BeforeEach
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

	@Test
	public void testEvaluate_missingDynamicRegistrationRequest(){
		assertThrows(ConditionError.class, () -> {
			env.putString("redirect_uri", "https://example1.org/redirect");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingRedirectUriInEnvironment(){
		assertThrows(ConditionError.class, () -> {
			env.putObject("dynamic_registration_request", dynamicRegistrationRequest);
			cond.execute(env);
		});
	}
}
