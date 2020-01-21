package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OIDCCValidateDynamicRegistrationRedirectUris_UnitTest
{

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private OIDCCValidateDynamicRegistrationRedirectUris cond;



	@Before
	public void setUp() throws Exception {

		cond = new OIDCCValidateDynamicRegistrationRedirectUris();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {
		JsonArray jsonArray = new JsonArray();
		jsonArray.add("https://openid-client.local/cb");
		jsonArray.add("https://client.example.org/callback");
		JsonObject dynRegRequest = new JsonObject();
		dynRegRequest.add("redirect_uris", jsonArray);
		env.putObject("dynamic_registration_request", dynRegRequest);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_emptyArray() {

		JsonArray jsonArray = new JsonArray();
		JsonObject dynRegRequest = new JsonObject();
		dynRegRequest.add("redirect_uris", jsonArray);
		env.putObject("dynamic_registration_request", dynRegRequest);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_string() {

		JsonObject dynRegRequest = new JsonObject();
		dynRegRequest.addProperty("redirect_uris", "https://client.example.org/callback");
		env.putObject("dynamic_registration_request", dynRegRequest);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidUri() {

		JsonArray jsonArray = new JsonArray();
		jsonArray.add("https://openid-client.local/cb");
		jsonArray.add("invalid uri 910!");
		JsonObject dynRegRequest = new JsonObject();
		dynRegRequest.add("redirect_uris", jsonArray);
		env.putObject("dynamic_registration_request", dynRegRequest);
		cond.execute(env);

	}
}
