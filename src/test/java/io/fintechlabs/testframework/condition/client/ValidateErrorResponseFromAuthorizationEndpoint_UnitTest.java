package io.fintechlabs.testframework.condition.client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author ddrysdale
 *
 */

@RunWith(MockitoJUnitRunner.class)
public class ValidateErrorResponseFromAuthorizationEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;
	private ValidateErrorResponseFromAuthorizationEndpoint cond;

	private String errorNoneNoOptionalFields = "{\"error\":\"access_denied\",\"state\":\"oObb8SUBuC\"}";
	private String errorNoneAllOptionalFields = "{\"error\":\"access_denied\",\"error_description\":\"incorrect credentials\",\"state\":\"oObb8SUBuC\", \"error_uri\":\"http://anerror.com\"}";
	private String errorTooManyFields = "{\"error\":\"access_denied\",\"error_description\":\"incorrect credentials\",\"state\":\"oObb8SUBuC\", \"error_uri\":\"http://anerror.com\",\"extraField\":\"Illegal Extra Field\"}";
	private String errorErrorMissing = "{\"error_description\":\"incorrect credentials\",\"state\":\"oObb8SUBuC\", \"error_uri\":\"http://anerror.com\"}";
	private String errorStateMissing = "{\"error\":\"access_denied\",\"error_description\":\"incorrect credentials\", \"error_uri\":\"http://anerror.com\"}";

	private String stateGood = "oObb8SUBuC";
	private String stateWrong = "BrokenDunk";

	/*
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new ValidateErrorResponseFromAuthorizationEndpoint("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	private void addState(String globalStateToAdd) {
		env.putString("state", globalStateToAdd);
	}

	private void doTestString(String stringToTest) {
		JsonObject jsonErrorNoneNoOptionalFields = new JsonParser().parse(stringToTest).getAsJsonObject();
		env.putObject("authorization_endpoint_response", jsonErrorNoneNoOptionalFields);
		cond.evaluate(env);
	}

	/**
	 * Test method for
	 * {@link ValidateErrorResponseFromAuthorizationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noErrorNoOptionalFieldsGoodState() {
		addState(stateGood);
		doTestString(errorNoneNoOptionalFields);
	}

	/**
	 * Test method for
	 * {@link ValidateErrorResponseFromAuthorizationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noErrorAllOptionalFieldsGoodState() {
		addState(stateGood);
		doTestString(errorNoneAllOptionalFields);
	}

	/**
	 * Test method for
	 * {@link ValidateErrorResponseFromAuthorizationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_noErrorNoOptionalFieldsWrongState() {
		addState(stateWrong);
		doTestString(errorNoneNoOptionalFields);
	}

	/**
	 * Test method for
	 * {@link ValidateErrorResponseFromAuthorizationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_noErrorAllOptionalFieldsWrongState() {
		addState(stateWrong);
		doTestString(errorNoneAllOptionalFields);
	}

	/**
	 * Test method for
	 * {@link ValidateErrorResponseFromAuthorizationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_errorMissing() {
		doTestString(errorErrorMissing);
	}

	/**
	 * Test method for
	 * {@link ValidateErrorResponseFromAuthorizationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_stateMissing() {
		doTestString(errorStateMissing);
	}

	/**
	 * Test method for
	 * {@link ValidateErrorResponseFromAuthorizationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_errorTooManyFields() {
		doTestString(errorTooManyFields);
	}
}
