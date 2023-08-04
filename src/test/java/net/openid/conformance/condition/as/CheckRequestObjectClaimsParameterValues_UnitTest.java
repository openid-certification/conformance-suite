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
public class CheckRequestObjectClaimsParameterValues_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckRequestObjectClaimsParameterValues cond;


	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CheckRequestObjectClaimsParameterValues();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link CheckRequestObjectClaimsParameterValues#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noErrors() {
		// All claims have valid values.
		JsonObject authRequestClaims = JsonParser.parseString("{" +
			"    \"claims\": {" +
			"	 \"claims\": {" +
			"	     \"userinfo\": {" +
			"	     }," +
			"	     \"id_token\": {" +
			"	     }" +
			"	 }" +
			"    }" +
			"}")
		.getAsJsonObject();

		env.putObject("authorization_request_object", authRequestClaims);

		cond.execute(env);
	}

	/**
	 * Test method for {@link CheckRequestObjectClaimsParameterValues#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidClaimValue() {
		// The 'userinfo' claim value is not the required JsonObject
		JsonObject authRequestClaims = JsonParser.parseString("{" +
			"    \"claims\": {" +
			"	 \"claims\": {" +
			"	     \"userinfo\": \"string\"," +
			"	     \"id_token\": {" +
			"	     }" +
			"	 }" +
			"    }" +
			"}")
		.getAsJsonObject();

		env.putObject("authorization_request_object", authRequestClaims);

		cond.execute(env);
	}
}
