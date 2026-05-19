package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckForUnexpectedOpenIdClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckForUnexpectedOpenIdClaims cond;


	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new CheckForUnexpectedOpenIdClaims();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link CheckForUnexpectedClaimsInClaimsParameter#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noErrors() {
		// All claims are valid.
		JsonObject authRequestClaims = JsonParser.parseString("{" +
			"	 \"claims\": {" +
			"	     \"userinfo\": {" +
			"		 \"family_name\": {" +
			"		 }" +
			"	     }," +
			"	     \"id_token\": {" +
			"	     }" +
			"	 }" +
			"}")
		.getAsJsonObject();

		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, authRequestClaims);

		cond.execute(env);
	}

	/**
	 * Test method for {@link CheckForUnexpectedClaimsInClaimsParameter#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_invalidClaim() {
		assertThrows(ConditionError.class, () -> {
			// The 'family name' claim is invalid.
			JsonObject authRequestClaims = JsonParser.parseString("{" +
				"	 \"claims\": {" +
				"	     \"userinfo\": {" +
				"		 \"family name\": {" +
				"		 }" +
				"	     }," +
				"	     \"id_token\": {" +
				"	     }" +
				"	 }" +
				"}")
				.getAsJsonObject();

			env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, authRequestClaims);

			cond.execute(env);
		});
	}
}
