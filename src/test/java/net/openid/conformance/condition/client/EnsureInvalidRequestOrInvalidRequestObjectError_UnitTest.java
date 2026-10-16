package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
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

@ExtendWith(MockitoExtension.class)
public class EnsureInvalidRequestOrInvalidRequestObjectError_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureInvalidRequestOrInvalidRequestObjectError cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureInvalidRequestOrInvalidRequestObjectError();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void setError(String error) {
		JsonObject authorizationEndpointResponse = new JsonObject();
		if (error != null) {
			authorizationEndpointResponse.addProperty("error", error);
		}
		env.putObject("authorization_endpoint_response", authorizationEndpointResponse);
	}

	@Test
	public void testEvaluate_missingErrorField() {
		setError(null);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_otherError() {
		setError("access_denied");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_invalidRequest() {
		setError("invalid_request");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_invalidRequestObject() {
		setError("invalid_request_object");
		cond.execute(env);
	}

}
