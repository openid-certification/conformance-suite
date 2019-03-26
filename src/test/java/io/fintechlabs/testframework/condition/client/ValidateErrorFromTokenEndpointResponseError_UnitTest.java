package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ValidateErrorFromTokenEndpointResponseError_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateErrorFromTokenEndpointResponseError cond;

	private JsonObject tokenEndpointResponse;

	@Before
	public void setUp() throws Exception {
		cond = new ValidateErrorFromTokenEndpointResponseError("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		tokenEndpointResponse = new JsonParser().parse("{"
			+ "\"error_description\":\"[A200308] The end-user has not been authenticated yet.\","
			+ "\"error\":\"authorization_pending\","
			+ "\"error_uri\":\"https://www.authlete.com/documents/apis/result_codes#A200308\"}").getAsJsonObject();

		env.putObject("token_endpoint_response", tokenEndpointResponse);
	}

	/**
	 * Test method by input string which has value with suffix include characters outside the set %x20-21 / %x23-5B / %x5D-7E
	 */
	@Test
	public void testIsValidFieldFormat_Invalid() {
		String str = "Test string invalid";

		// Test method by input string which has value with suffix include characters inside %X00-X1F (HEX)
		int HEX_X00_ASCII = 0;
		int HEX_X1F_ASCII = 31;
		for (int i = HEX_X00_ASCII; i <= HEX_X1F_ASCII; i++) {
			assertThat(cond.isValidFieldFormat(str + (char) i)).isFalse();
		}

		// Test method by input string which has value with suffix include character %X22 (HEX)
		int HEX_X22_ASCII = 34;
		assertThat(cond.isValidFieldFormat(str + (char) HEX_X22_ASCII)).isFalse();

		// Test method by input string which has value with suffix include character %X5C (HEX)
		int HEX_X5C_ASCII = 92;
		assertThat(cond.isValidFieldFormat(str + (char) HEX_X5C_ASCII)).isFalse();

		// Test method by input string which has value with suffix include characters inside %X7F-XFF (HEX)
		int HEX_X7F_ASCII = 127;
		int HEX_XFF_ASCII = 255;
		for (int i = HEX_X7F_ASCII; i <= HEX_XFF_ASCII; i++) {
			assertThat(cond.isValidFieldFormat(str + (char) i)).isFalse();
		}
	}

	/**
	 * Test method by input string which has value with suffix include characters inside the set %x20-21 / %x23-5B / %x5D-7E
	 */
	@Test
	public void testIsValidFieldFormat_Valid() {
		String str = "Test string valid";

		// Test method by input string which has value with suffix include characters inside %X20-X21 (HEX)
		int HEX_X20_ASCII = 32;
		int HEX_X21_ASCII = 33;
		for (int i = HEX_X20_ASCII; i <= HEX_X21_ASCII; i++) {
			assertThat(cond.isValidFieldFormat(str + (char) i)).isTrue();
		}

		// Test method by input string which has value with suffix include characters inside %X23-x5b (HEX)
		int HEX_X23_ASCII = 35;
		int HEX_X5B_ASCII = 91;
		for (int i = HEX_X23_ASCII; i <= HEX_X5B_ASCII; i++) {
			assertThat(cond.isValidFieldFormat(str + (char) i)).isTrue();
		}

		// Test method by input string which has value with suffix include characters inside %X5D-X7E (HEX)
		int HEX_X5D_ASCII = 93;
		int HEX_X7E_ASCII = 126;
		for (int i = HEX_X5D_ASCII; i <= HEX_X7E_ASCII; i++) {
			assertThat(cond.isValidFieldFormat(str + (char) i)).isTrue();
		}
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_NotExistTokenEndpointResponse() {
		env.removeObject("token_endpoint_response");
		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_ErrorValueEmpty() {
		tokenEndpointResponse.addProperty("error", "");
		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_ErrorValueInvalid() {
		tokenEndpointResponse.addProperty("error", "authorization_pending\"");
		cond.evaluate(env);
	}

	@Test
	public void testEvaluate_ErrorValueValid() {
		tokenEndpointResponse.addProperty("error", "authorization_pending");
		cond.evaluate(env);
	}
}
