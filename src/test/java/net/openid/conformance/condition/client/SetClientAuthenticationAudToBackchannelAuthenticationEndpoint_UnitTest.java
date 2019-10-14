package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SetClientAuthenticationAudToBackchannelAuthenticationEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private SetClientAuthenticationAudToBackchannelAuthenticationEndpoint cond;

	private JsonObject server;

	private JsonObject claims;

	@Before
	public void setUp() throws Exception {

		cond = new SetClientAuthenticationAudToBackchannelAuthenticationEndpoint();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		claims = new JsonParser().parse("{"
			+ "\"iss\":\"294570236252\","
			+ "\"exp\":\"1557395297\","
			+ "\"iat\":\"1557394997\""
			+ "}").getAsJsonObject();

		env.putObject("client_assertion_claims", claims);

		server = new JsonParser().parse("{"
			+ "\"backchannel_authentication_endpoint\":\"https://fapidev-as.authlete.net/api/backchannel/authentication\""
			+ "}").getAsJsonObject();

		env.putObject("server", server);

	}

	@Test
	public void testEvaluate_noError() {

		cond.execute(env);

		verify(env, atLeastOnce()).getElementFromObject("server", "backchannel_authentication_endpoint");
		verify(env, atLeastOnce()).getObject("client_assertion_claims");

		assertEquals(env.getElementFromObject("client_assertion_claims", "aud"),
					 env.getElementFromObject("server", "backchannel_authentication_endpoint"));

	}

}
