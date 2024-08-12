package net.openid.conformance.condition.as;

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
public class CheckForUnexpectedClaimsInRequestObject_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckForUnexpectedClaimsInRequestObject cond;


	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new CheckForUnexpectedClaimsInRequestObject();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link CheckForUnexpectedClaimsInRequestObject#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noErrors() {

		// All claims are valid.
		JsonObject authRequestClaims = JsonParser.parseString("{" +
			"    \"claims\": {" +
			"	\"client_id\": \"52480754053\"," +
			"	\"iss\": \"52480754053\"" +
			"    }" +
			"}")
		.getAsJsonObject();

		env.putObject("authorization_request_object", authRequestClaims);

		cond.execute(env);

	}

	/**
	 * Test method for {@link CheckForUnexpectedClaimsInRequestObject#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_misspeltClaim() {
		assertThrows(ConditionError.class, () -> {
			// The 'nonce' claim is misspelt.
			JsonObject authRequestClaims = JsonParser.parseString("{" +
				"    \"claims\": {" +
				"	\"client_id\": \"52480754053\"," +
				"	\"iss\": \"52480754053\"," +
				"	\"noncex\": \"rC3y9vbmmJ\"" +
				"    }" +
				"}")
				.getAsJsonObject();

			env.putObject("authorization_request_object", authRequestClaims);

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link CheckForUnexpectedClaimsInRequestObject#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_invalidClaim() {
		assertThrows(ConditionError.class, () -> {
			// The authentication request 'request_uri'  parameter should not be in the request_object.
			JsonObject authRequestClaims = JsonParser.parseString("{" +
				"    \"claims\": {" +
				"	\"client_id\": \"52480754053\"," +
				"	\"iss\": \"52480754053\"," +
				"	\"request_uri\": \"https://example.com/request\"" +
				"    }" +
				"}")
				.getAsJsonObject();

			env.putObject("authorization_request_object", authRequestClaims);

			cond.execute(env);
		});
	}
}
