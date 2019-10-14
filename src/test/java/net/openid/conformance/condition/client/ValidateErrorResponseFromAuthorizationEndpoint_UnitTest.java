package net.openid.conformance.condition.client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class ValidateErrorResponseFromAuthorizationEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;
	private ValidateErrorResponseFromAuthorizationEndpoint cond;

	private String errorNoneNoOptionalFields = "{\"error\":\"access_denied\",\"state\":\"oObb8SUBuC\",\"session_state\":\"4912fd505631d79b1d39f1e537679d1931558c5347707e04ae6483dda4d8fc85.cE2Jqk7V6cv_jLcoWXa3yQ\"}";
	private String errorNoneAllOptionalFields = "{\"error\":\"access_denied\",\"error_description\":\"incorrect credentials\",\"state\":\"oObb8SUBuC\", \"error_uri\":\"http://anerror.com\",\"session_state\":\"4912fd505631d79b1d39f1e537679d1931558c5347707e04ae6483dda4d8fc85.cE2Jqk7V6cv_jLcoWXa3yQ\"}";
	private String errorTooManyFields = "{\"error\":\"access_denied\",\"error_description\":\"incorrect credentials\",\"state\":\"oObb8SUBuC\", \"error_uri\":\"http://anerror.com\",\"extraField\":\"Illegal Extra Field\",\"session_state\":\"4912fd505631d79b1d39f1e537679d1931558c5347707e04ae6483dda4d8fc85.cE2Jqk7V6cv_jLcoWXa3yQ\"}";
	private String errorErrorMissing = "{\"error_description\":\"incorrect credentials\",\"state\":\"oObb8SUBuC\", \"error_uri\":\"http://anerror.com\"}";

	private String stateGood = "oObb8SUBuC";

	/*
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		cond = new ValidateErrorResponseFromAuthorizationEndpoint();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void addState(String globalStateToAdd) {
		env.putString("state", globalStateToAdd);
	}

	private void doTestString(String stringToTest) {
		JsonObject jsonErrorNoneNoOptionalFields = new JsonParser().parse(stringToTest).getAsJsonObject();
		env.putObject("authorization_endpoint_response", jsonErrorNoneNoOptionalFields);
		cond.execute(env);
	}

	/**
	 * Test method for
	 * {@link ValidateErrorResponseFromAuthorizationEndpoint#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noErrorNoOptionalFieldsGoodState() {
		addState(stateGood);
		doTestString(errorNoneNoOptionalFields);
	}

	/**
	 * Test method for
	 * {@link ValidateErrorResponseFromAuthorizationEndpoint#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noErrorAllOptionalFieldsGoodState() {
		addState(stateGood);
		doTestString(errorNoneAllOptionalFields);
	}

	/**
	 * Test method for
	 * {@link ValidateErrorResponseFromAuthorizationEndpoint#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_errorMissing() {
		doTestString(errorErrorMissing);
	}

	/**
	 * Test method for
	 * {@link ValidateErrorResponseFromAuthorizationEndpoint#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_errorTooManyFields() {
		doTestString(errorTooManyFields);
	}
}
