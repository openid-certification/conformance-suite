package net.openid.conformance.condition.client;

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
public class ValidateErrorResponseFromBackchannelAuthenticationEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;
	private ValidateErrorResponseFromBackchannelAuthenticationEndpoint cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateErrorResponseFromBackchannelAuthenticationEndpoint();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void doTestString(String stringToTest) {
		JsonObject jsonErrorNoneNoOptionalFields = JsonParser.parseString(stringToTest).getAsJsonObject();
		env.putObject("backchannel_authentication_endpoint_response", jsonErrorNoneNoOptionalFields);
		cond.execute(env);
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

	@Test
	public void testEvaluate_errorMissing() {
		assertThrows(ConditionError.class, () -> {
			String errorErrorMissing = "{\"error_description\":\"incorrect credentials\",\"error_uri\":\"http://anerror.com\"}";
			doTestString(errorErrorMissing);
		});
	}

	@Test
	public void testEvaluate_errorTooManyFields() {
		assertThrows(ConditionError.class, () -> {
			String errorTooManyFields = "{\"error\":\"access_denied\",\"error_description\":\"incorrect credentials\",\"error_uri\":\"http://anerror.com\",\"extraField\":\"Illegal Extra Field\"}";
			doTestString(errorTooManyFields);
		});
	}
}
