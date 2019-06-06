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

@RunWith(MockitoJUnitRunner.class)
public class ValidateErrorResponseFromBackchannelAuthenticationEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;
	private ValidateErrorResponseFromBackchannelAuthenticationEndpoint cond;

	@Before
	public void setUp() throws Exception {
		cond = new ValidateErrorResponseFromBackchannelAuthenticationEndpoint();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void doTestString(String stringToTest) {
		JsonObject jsonErrorNoneNoOptionalFields = new JsonParser().parse(stringToTest).getAsJsonObject();
		env.putObject("backchannel_authentication_endpoint_response", jsonErrorNoneNoOptionalFields);
		cond.evaluate(env);
	}

	@Test
	public void testEvaluate_caseGoodWithNoOptionalFields() {
		String errorNoneNoOptionalFields = "{\"error\":\"access_denied\"}";
		doTestString(errorNoneNoOptionalFields);
	}

	@Test
	public void testEvaluate_caseGoodWithrAllOptionalFieldsGoodState() {
		String errorNoneAllOptionalFields = "{\"error\":\"access_denied\",\"error_description\":\"incorrect credentials\",\"error_uri\":\"http://anerror.com\"}";
		doTestString(errorNoneAllOptionalFields);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_errorMissing() {
		String errorErrorMissing = "{\"error_description\":\"incorrect credentials\",\"error_uri\":\"http://anerror.com\"}";
		doTestString(errorErrorMissing);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_errorTooManyFields() {
		String errorTooManyFields = "{\"error\":\"access_denied\",\"error_description\":\"incorrect credentials\",\"error_uri\":\"http://anerror.com\",\"extraField\":\"Illegal Extra Field\"}";
		doTestString(errorTooManyFields);
	}
}
