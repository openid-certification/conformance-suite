package net.openid.conformance.condition.as;

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

@ExtendWith(MockitoExtension.class)
public class CheckForUnexpectedParametersInSignedPAREndpointRequestWithClientAssertion_UnitTest {
	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckForUnexpectedParametersInSignedPAREndpointRequestWithClientAssertion cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckForUnexpectedParametersInSignedPAREndpointRequestWithClientAssertion();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putParRequest(String json) {
		JsonObject parRequest = JsonParser.parseString(json).getAsJsonObject();
		env.putObject("par_endpoint_http_request", parRequest);
	}

	@Test
	public void testEvaluate_requestAndClientAssertion() {

		putParRequest(
		"""
		{
			"body_form_params" : {
				"request" : "abcd1234",
				"client_assertion" : "abcd1234",
				"client_assertion_type" : "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
			}
		}
		""");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_requestClientAssertionAndClientId() {

		putParRequest(
		"""
		{
			"body_form_params" : {
				"request" : "abcd1234",
				"client_id" : "client",
				"client_assertion" : "abcd1234",
				"client_assertion_type" : "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
			}
		}
		""");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_authorizationRequestParameter() {

		putParRequest(
		"""
		{
			"body_form_params" : {
				"request" : "abcd1234",
				"client_assertion" : "abcd1234",
				"client_assertion_type" : "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
				"dpop_jkt" : "abcd1234"
			}
		}
		""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_noParams() {

		putParRequest(
		"""
		{
		}
		""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
