package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class AddLoginHintFromConfigurationToAuthorizationEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddLoginHintFromConfigurationToAuthorizationEndpointRequest cond;

	@Before
	public void setUp() throws Exception {

		cond = new AddLoginHintFromConfigurationToAuthorizationEndpointRequest();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		JsonObject config = new JsonObject();
		config.add("server", new JsonParser().parse("{\"login_hint\": \"flibble\"}"));

		env.putObject("config", config);

		JsonObject server = new JsonObject();
		server.addProperty("issuer", "https://fapidev-as.authlete.net/");

		env.putObject("server", server);

		env.putObject("authorization_endpoint_request", new JsonObject());
	}

	@Test
	public void testEvaluate_caseConfigSupplied() {

		cond.execute(env);

		assertEquals("flibble", env.getString("authorization_endpoint_request", "login_hint"));

	}

	@Test
	public void testEvaluate_caseNoConfig() {

		env.putObject("config", new JsonObject());

		cond.execute(env);

		assertEquals("buffy@fapidev-as.authlete.net", env.getString("authorization_endpoint_request", "login_hint"));

	}

}
