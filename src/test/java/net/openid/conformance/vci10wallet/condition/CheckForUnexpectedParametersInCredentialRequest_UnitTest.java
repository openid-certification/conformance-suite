package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.condition.AbstractVciUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
public class CheckForUnexpectedParametersInCredentialRequest_UnitTest extends AbstractVciUnitTest {

	private CheckForUnexpectedParametersInCredentialRequest cond;

	@Mock
	private TestInstanceEventLog eventLog;

	private Environment env;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckForUnexpectedParametersInCredentialRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	public void testEvaluate_noWarningWhenNoUnknownProperties() {
		String json = """
			{
			  "credential_configuration_id": "UniversityDegreeCredential",
			  "proof": {
			    "proof_type": "jwt",
			    "jwt": "eyJ..."
			  }
			}
			""";
		putCredentialRequest(json);
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_unknownTopLevelProperty() {
		String json = """
			{
			  "credential_configuration_id": "UniversityDegreeCredential",
			  "unexpected_field": "boom"
			}
			""";
		putCredentialRequest(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertUnknownPropertyAtPath(data, "$.unexpected_field");
	}

	@Test
	public void testEvaluate_structuralErrorsDoNotWarn() {
		// Missing required field (credential_identifier or credential_configuration_id) is structural, not unknown property
		String json = """
			{
			  "proof": {
			    "proof_type": "jwt"
			  }
			}
			""";
		putCredentialRequest(json);
		assertDoesNotThrow(() -> cond.execute(env));
	}

	private void putCredentialRequest(String json) {
		JsonObject body = JsonParser.parseString(json).getAsJsonObject();
		JsonObject incomingRequest = new JsonObject();
		incomingRequest.add("body_json", body);
		env.putObject("incoming_request", incomingRequest);
	}
}
