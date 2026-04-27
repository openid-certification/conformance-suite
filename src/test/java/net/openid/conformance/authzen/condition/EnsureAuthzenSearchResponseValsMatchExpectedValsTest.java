package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EnsureAuthzenSearchResponseValsMatchExpectedValsTest {
	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureAuthzenSearchResponseValsMatchExpectedVals cond;

	JsonObject expectedResponse;

	JsonObject actualResponse;


	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureAuthzenSearchResponseValsMatchExpectedVals();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		expectedResponse = new JsonObject();
		actualResponse = new JsonObject();

		env.putObject("authzen_search_endpoint_expected_response", expectedResponse);
		env.putObject("authzen_search_endpoint_response", actualResponse);
	}


	void performTest(String expectedJson, String actualJson) {
		JsonArray expected = JsonParser.parseString(expectedJson).getAsJsonArray();
		JsonArray actual = JsonParser.parseString(actualJson).getAsJsonArray();

		expectedResponse.add("results", expected);
		actualResponse.add("results", actual);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_single_value_response_noError() {
		String expectedJson = """
		[
			{
				"name": "view"
			}
		]""";

		String actualJson = """
		[
			{
				"name": "view"
			}
		]""";

		performTest(expectedJson, actualJson);
	}

	@Test
	public void testEvaluate_multiple_value_response_noError() {
		String expectedJson = """
		[
			{
				"name": "view",
				"test": "test1"
			}
		]""";

		String actualJson = """
		[
			{
				"name": "view",
				"test": "test1"
			}
		]""";

		performTest(expectedJson, actualJson);
	}

	@Test
	public void testEvaluate_empty_response_noError() {
		String expectedJson = """
		[
		]""";

		String actualJson = """
		[
		]""";

		performTest(expectedJson, actualJson);
	}

	@Test
	public void testEvaluate_error_missing_response() {
		String expectedJson = """
		[
			{
				"name": "view"
			}
		]""";

		String actualJson = """
		[
		]""";

		Throwable e = assertThrows(ConditionError.class, () -> performTest(expectedJson, actualJson));
		assertTrue(e.getMessage().contains("Search result is missing expected elements"));
	}

	@Test
	public void testEvaluate_error_empty_expected_response() {
		String expectedJson = """
		[
		]""";

		String actualJson = """
		[
			{
				"name": "view"
			}
		]""";

		Throwable e = assertThrows(ConditionError.class, () -> performTest(expectedJson, actualJson));
		assertTrue(e.getMessage().contains("Search result contains unexpected elements"));
	}

	@Test
	public void testEvaluate_error_response_size_less_than_expected() {
		String expectedJson = """
		[
			{
				"name": "view"
			},
			{
				"name": "view2"
			}
		]""";

		String actualJson = """
		[
			{
				"name": "view"
			}
		]""";

		Throwable e = assertThrows(ConditionError.class, () -> performTest(expectedJson, actualJson));
		assertTrue(e.getMessage().contains("Search result is missing expected elements"));
	}

	@Test
	public void testEvaluate_error_response_more_than_expected_size() {
		String expectedJson = """
		[
			{
				"name": "view"
			}
		]""";

		String actualJson = """
		[
			{
				"name": "view"
			},
			{
				"name": "view2"
			}
		]""";

		Throwable e = assertThrows(ConditionError.class, () -> performTest(expectedJson, actualJson));
		assertTrue(e.getMessage().contains("Search result contains unexpected elements"));
	}

	@Test
	public void testEvaluate_duplicate_expected_values_noError() {
		String expectedJson = """
		[
			{
				"name": "view"
			},
			{
				"name": "view2"
			},
			{
				"name": "view"
			}
		]""";

		String actualJson = """
		[
			{
				"name": "view"
			},
			{
				"name": "view2"
			}
		]""";

		performTest(expectedJson, actualJson);
	}

	@Test
	public void testEvaluate_duplicate_response_values_noError() {
		String expectedJson = """
		[
			{
				"name": "view"
			}
		]""";

		String actualJson = """
		[
			{
				"name": "view"
			},
			{
				"name": "view"
			}
		]""";

		performTest(expectedJson, actualJson);
	}

	@Test
	public void testEvaluate_error_missing_expected_response() {
		String expectedJson = """
		[
			{
				"name": "view"
			},
			{
				"test": "test1"
			}
		]""";

		String actualJson = """
		[
			{
				"name": "view"
			}
		]""";

		Throwable e = assertThrows(ConditionError.class, () -> performTest(expectedJson, actualJson));
		assertTrue(e.getMessage().contains("Search result is missing expected elements"));
	}

	@Test
	public void testEvaluate_error_unexpected_response() {
		String expectedJson = """
		[
			{
				"name": "view"
			}
		]""";

		String actualJson = """
		[
			{
				"name": "view"
			},
			{
				"test": "test1"
			}
		]""";

		Throwable e = assertThrows(ConditionError.class, () -> performTest(expectedJson, actualJson));
		assertTrue(e.getMessage().contains("Search result contains unexpected elements"));
	}

	@Test
	public void testEvaluate_error_unexpected_and_missing_response() {
		String expectedJson = """
		[
			{
				"name": "view"
			},
			{
				"test": "test2"
			}
		]""";

		String actualJson = """
		[
			{
				"name": "view"
			},
			{
				"test": "test1"
			}
		]""";

		Throwable e = assertThrows(ConditionError.class, () -> performTest(expectedJson, actualJson));
		assertTrue(e.getMessage().contains("Search result does not match"));
	}

}
