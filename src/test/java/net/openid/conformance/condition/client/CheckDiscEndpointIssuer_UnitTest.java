package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
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
public class CheckDiscEndpointIssuer_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckDiscEndpointIssuer cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new CheckDiscEndpointIssuer();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		JsonObject config = new JsonObject();
		config.add("server", JsonParser.parseString("{\"discoveryUrl\": \"https://login.microsoftonline.com/5608b4e0-e26d-4bd1-8a1a-d57d7ae2af8c/v2.0/.well-known/openid-configuration\"}"));

		env.putObject("config", config);
	}

	@Test
	public void testEvaluate_caseMissingIssuer() {
		assertThrows(ConditionError.class, () -> {

			JsonObject server = new JsonObject();

			env.putObject("server", server);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_caseNoMatching() {
		assertThrows(ConditionError.class, () -> {

			JsonObject server = new JsonObject();
			server.addProperty("issuer", "https://fapidev-as.authlete.net/");

			env.putObject("server", server);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_caseInvalidIssuer() {
		assertThrows(ConditionError.class, () -> {

			JsonObject server = new JsonObject();
			server.addProperty("issuer", "http:/fapidev-as.authlete.net/");

			env.putObject("server", server);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_caseMatching() {

		JsonObject server = new JsonObject();
		server.addProperty("issuer", "https://login.microsoftonline.com/5608b4e0-e26d-4bd1-8a1a-d57d7ae2af8c/v2.0");

		env.putObject("server", server);

		cond.execute(env);

	}

}
