package net.openid.conformance.condition.common;

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

@ExtendWith(MockitoExtension.class)
public class CheckAuthorizationEndpointIsValidUri_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckAuthorizationEndpointIsValidUri cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckAuthorizationEndpointIsValidUri();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_httpsUrl() {
		JsonObject server = JsonParser.parseString(
			"{\"authorization_endpoint\": \"https://example.com/authorize\"}"
		).getAsJsonObject();
		env.putObject("server", server);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_openid4vpScheme() {
		JsonObject server = JsonParser.parseString(
			"{\"authorization_endpoint\": \"openid4vp://authorize\"}"
		).getAsJsonObject();
		env.putObject("server", server);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_openid4vpBareScheme() {
		JsonObject server = JsonParser.parseString(
			"{\"authorization_endpoint\": \"openid4vp://\"}"
		).getAsJsonObject();
		env.putObject("server", server);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_httpUrl() {
		JsonObject server = JsonParser.parseString(
			"{\"authorization_endpoint\": \"http://example.com/authorize\"}"
		).getAsJsonObject();
		env.putObject("server", server);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingAuthorizationEndpoint() {
		JsonObject server = JsonParser.parseString("{}").getAsJsonObject();
		env.putObject("server", server);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_emptyAuthorizationEndpoint() {
		JsonObject server = JsonParser.parseString(
			"{\"authorization_endpoint\": \"\"}"
		).getAsJsonObject();
		env.putObject("server", server);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_blankAuthorizationEndpoint() {
		JsonObject server = JsonParser.parseString(
			"{\"authorization_endpoint\": \"   \"}"
		).getAsJsonObject();
		env.putObject("server", server);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_invalidUri() {
		JsonObject server = JsonParser.parseString(
			"{\"authorization_endpoint\": \"not a valid uri with spaces[\"}"
		).getAsJsonObject();
		env.putObject("server", server);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_noScheme() {
		JsonObject server = JsonParser.parseString(
			"{\"authorization_endpoint\": \"example.com/authorize\"}"
		).getAsJsonObject();
		env.putObject("server", server);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

}
