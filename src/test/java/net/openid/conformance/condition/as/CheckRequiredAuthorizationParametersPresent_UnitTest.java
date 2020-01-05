package net.openid.conformance.condition.as;

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


@RunWith(MockitoJUnitRunner.class)
public class CheckRequiredAuthorizationParametersPresent_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckRequiredAuthorizationParametersPresent cond;

	private String responseType = "code";

	private String clientId = "test-client-id-346334adgdsfgdfg3425";

	private String redirectUri = "http://localhost:44444/";

	private String scope = "openid accounts";

	private JsonObject params;

	@Before
	public void setUp() throws Exception {

		cond = new CheckRequiredAuthorizationParametersPresent();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO, new String[0]);

		params = new JsonParser().parse("{"
			+ "\"response_type\":\"" + responseType + "\","
			+ "\"client_id\":\"" + clientId + "\","
			+ "\"scope\":\"" + scope + "\","
			+ "\"redirect_uri\":\"" + redirectUri + "\""
			+ "}").getAsJsonObject();

	}

	private void addEndpointRequest(Environment env, JsonObject params) {

		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, params);
	}


	@Test
	public void testEvaluate_noError() {

		addEndpointRequest(env, params);

		cond.execute(env);
	}

	@Test (expected = ConditionError.class)
	public void testEvaluate_NullParameter()
	{
		addEndpointRequest(env, params);

		params.remove("response_type");

		cond.execute(env);
	}

	@Test (expected = ConditionError.class)
	public void testEvaluate_Remove2Parameters()
	{
		addEndpointRequest(env, params);

		params.remove("response_type");
		params.remove("scope");

		cond.execute(env);
	}

}
