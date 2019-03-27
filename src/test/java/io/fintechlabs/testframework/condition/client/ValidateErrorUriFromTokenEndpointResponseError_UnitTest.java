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
public class ValidateErrorUriFromTokenEndpointResponseError_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateErrorUriFromTokenEndpointResponseError cond;

	private JsonObject tokenEndpointResponse;

	private static final String ERROR_URI_FIELD_PATTERN_VALID = "[\\x21\\x23-\\x5B\\x5D-\\x7E]+";

	@Before
	public void setUp() throws Exception {
		cond = new ValidateErrorUriFromTokenEndpointResponseError("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		tokenEndpointResponse = new JsonParser().parse("{"
			+ "\"error_description\":\"[A200308] The end-user has not been authenticated yet.\","
			+ "\"error\":\"authorization_pending\","
			+ "\"error_uri\":\"https://www.authlete.com/documents/apis/result_codes#A200308\"}").getAsJsonObject();

		env.putObject("token_endpoint_response", tokenEndpointResponse);
	}

	/**
	 * Test method is invalid by pattern [\\x21\\x23-\\x5B\\x5D-\\x7E]+
	 */
	@Test
	public void testIsValidFieldFormat_Invalid() {
		String str = "TestStringInvalid";

		// Test method by input string which has value with suffix include characters inside %X00-X20 (HEX)
		int HEX_X00_ASCII = 0;
		int HEX_X20_ASCII = 32;
		for (int i = HEX_X00_ASCII; i <= HEX_X20_ASCII; i++) {
			assertThat(cond.isValidFieldFormat(str + (char) i, ERROR_URI_FIELD_PATTERN_VALID)).isFalse();
		}

		// Test method by input string which has value with suffix include character %X22 (HEX)
		int HEX_X22_ASCII = 34;
		assertThat(cond.isValidFieldFormat(str + (char) HEX_X22_ASCII, ERROR_URI_FIELD_PATTERN_VALID)).isFalse();

		// Test method by input string which has value with suffix include character %X5C (HEX)
		int HEX_X5C_ASCII = 92;
		assertThat(cond.isValidFieldFormat(str + (char) HEX_X5C_ASCII, ERROR_URI_FIELD_PATTERN_VALID)).isFalse();

		// Test method by input string which has value with suffix include characters inside %X7F-XFF (HEX)
		int HEX_X7F_ASCII = 127;
		int HEX_XFF_ASCII = 255;
		for (int i = HEX_X7F_ASCII; i <= HEX_XFF_ASCII; i++) {
			assertThat(cond.isValidFieldFormat(str + (char) i, ERROR_URI_FIELD_PATTERN_VALID)).isFalse();
		}
	}

	/**
	 * Test method is valid by pattern [\\x21\\x23-\\x5B\\x5D-\\x7E]+
	 */
	@Test
	public void testIsValidFieldFormat_Valid() {
		String str = "TestStringValid";

		// Test method by input string which has value with suffix include characters inside %X21 (HEX)
		int HEX_X21_ASCII = 33;
		assertThat(cond.isValidFieldFormat(str + (char) HEX_X21_ASCII, ERROR_URI_FIELD_PATTERN_VALID)).isTrue();

		// Test method by input string which has value with suffix include characters inside %X23-x5b (HEX)
		int HEX_X23_ASCII = 35;
		int HEX_X5B_ASCII = 91;
		for (int i = HEX_X23_ASCII; i <= HEX_X5B_ASCII; i++) {
			assertThat(cond.isValidFieldFormat(str + (char) i, ERROR_URI_FIELD_PATTERN_VALID)).isTrue();
		}

		// Test method by input string which has value with suffix include characters inside %X5D-X7E (HEX)
		int HEX_X5D_ASCII = 93;
		int HEX_X7E_ASCII = 126;
		for (int i = HEX_X5D_ASCII; i <= HEX_X7E_ASCII; i++) {
			assertThat(cond.isValidFieldFormat(str + (char) i, ERROR_URI_FIELD_PATTERN_VALID)).isTrue();
		}
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_NotExistTokenEndpointResponse() {
		env.removeObject("token_endpoint_response");
		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_ErrorUriCharacterInvalid() {
		tokenEndpointResponse.addProperty("error_uri", "https://www.authlete.com/documents/apis/result_codes#A200308\"");
		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_ErrorUriSyntaxInvalid() {
		tokenEndpointResponse.addProperty("error_uri", "/authlete/documents/apis/result_codes#A200308");
		cond.evaluate(env);
	}

	@Test
	public void testEvaluate_ErrorUriValid() {
		tokenEndpointResponse.addProperty("error_uri", "https://www.authlete.com/documents/apis/result_codes#A200308");
		cond.evaluate(env);
	}
}
