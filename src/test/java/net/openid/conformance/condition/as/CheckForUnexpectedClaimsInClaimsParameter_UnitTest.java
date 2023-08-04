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
public class CheckForUnexpectedClaimsInClaimsParameter_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckForUnexpectedClaimsInClaimsParameter cond;


	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CheckForUnexpectedClaimsInClaimsParameter();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link CheckForUnexpectedClaimsInClaimsParameter#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noErrors() {
		// All claims are valid.
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
	 * Test method for {@link CheckForUnexpectedClaimsInClaimsParameter#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidClaim() {
		// The 'email' claim is invalid.
		JsonObject authRequestClaims = JsonParser.parseString("{" +
			"    \"claims\": {" +
			"	 \"claims\": {" +
			"	     \"userinfo\": {" +
			"	     }," +
			"	     \"id_token\": {" +
			"	     }," +
			"	     \"email\": \"example@example.com\"" +
			"	 }" +
			"    }" +
			"}")
		.getAsJsonObject();

		env.putObject("authorization_request_object", authRequestClaims);

		cond.execute(env);
	}
}
