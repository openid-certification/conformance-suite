package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
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
class EnsureAuthzenSearchResponseValsMatchExpectedValsTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

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
	public void testEvaluate_noError() {
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
	public void testEvaluate_noError2() {
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
	public void testEvaluate_error_different_response_size() {
		String expectedJson = """
		[
			{
				"name": "view"
			},
			{
				"name2": "view"
			}
		]""";

		String actualJson = """
		[
			{
				"name": "view"
			}
		]""";

		assertThrows(ConditionError.class, () -> performTest(expectedJson, actualJson));
	}

	@Test
	public void testEvaluate_error_different_response_size_correct_values() {
		String expectedJson = """
		[
			{
				"name": "view"
			},
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


		assertThrows(ConditionError.class, () -> performTest(expectedJson, actualJson));
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


		assertThrows(ConditionError.class, () -> performTest(expectedJson, actualJson));
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


		assertThrows(ConditionError.class, () -> performTest(expectedJson, actualJson));
	}

}
