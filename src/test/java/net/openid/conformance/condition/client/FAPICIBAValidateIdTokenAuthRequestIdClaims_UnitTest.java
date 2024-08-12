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
public class FAPICIBAValidateIdTokenAuthRequestIdClaims_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private FAPICIBAValidateIdTokenAuthRequestIdClaims cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new FAPICIBAValidateIdTokenAuthRequestIdClaims();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_caseGoodEmpty() {
		assertThrows(NullPointerException.class, () -> {
			JsonObject requestParameters = JsonParser.parseString("{\"auth_req_id\":\"FlFNzv_88I2U4ELEhI3-STEtd-DDQFVD-_UqfRKgxrE\"}").getAsJsonObject();
			env.putObject("token_endpoint_request_form_parameters", requestParameters);

			JsonObject claims = JsonParser.parseString("{\"claims\":{}}").getAsJsonObject();
			env.putObject("id_token", claims);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_caseGood() {
		JsonObject requestParameters = JsonParser.parseString("{\"auth_req_id\":\"FlFNzv_88I2U4ELEhI3-STEtd-DDQFVD-_UqfRKgxrE\"}").getAsJsonObject();
		env.putObject("token_endpoint_request_form_parameters", requestParameters);

		JsonObject claims = JsonParser.parseString("{\"claims\":{\"urn:openid:params:jwt:claim:auth_req_id\": \"FlFNzv_88I2U4ELEhI3-STEtd-DDQFVD-_UqfRKgxrE\"}}").getAsJsonObject();
		env.putObject("id_token", claims);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_caseBad() {
		assertThrows(ConditionError.class, () -> {
			JsonObject requestParameters = JsonParser.parseString("{\"auth_req_id\":\"FlFNzv_88I2U4ELEhI3-STEtd-DDQFVD-_UqfRKgxrE\"}").getAsJsonObject();
			env.putObject("token_endpoint_request_form_parameters", requestParameters);

			JsonObject claims = JsonParser.parseString("{\"claims\":{\"urn:openid:params:jwt:claim:auth_req_id\": \"1c266114-a1be-4252-8ad1-04986c5b9ac1\"}}").getAsJsonObject();
			env.putObject("id_token", claims);

			cond.execute(env);

		});

	}
}
