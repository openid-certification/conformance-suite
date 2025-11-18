package net.openid.conformance.condition.as;

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
public class AustraliaConnectIdCheckForUnexpectedParametersInPAREndpointRequest_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AustraliaConnectIdCheckForUnexpectedParametersInPAREndpointRequest cond;
	@BeforeEach
	public void setUp() throws Exception {
		cond = new AustraliaConnectIdCheckForUnexpectedParametersInPAREndpointRequest();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {

		JsonObject parRequest= JsonParser.parseString(
		"""
		{
			"body_form_params" : {
				"request" : "abcd1234",
				"client_assertion" : "abcd1234",
				"client_assertion_type" : "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
			}
		}
		""").getAsJsonObject();

		env.putObject("par_endpoint_http_request", parRequest);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_invalidParam() {

		JsonObject parRequest= JsonParser.parseString(
		"""
		{
			"body_form_params" : {
				"request" : "abcd1234",
				"client_assertion" : "abcd1234",
				"client_assertion_type" : "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
				"invalid" : "invalid"
			}
		}
		""").getAsJsonObject();

		assertThrows(ConditionError.class, () -> {
			env.putObject("par_endpoint_http_request", parRequest);
			cond.execute(env);
		});

	}

	@Test
	public void testEvaluate_noParams() {

		JsonObject parRequest= JsonParser.parseString(
		"""
		{
		}
		""").getAsJsonObject();

		assertThrows(ConditionError.class, () -> {
			env.putObject("par_endpoint_http_request", parRequest);
			cond.execute(env);
		});

	}
}
