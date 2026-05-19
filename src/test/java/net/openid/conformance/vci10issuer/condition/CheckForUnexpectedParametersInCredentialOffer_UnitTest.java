package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
public class CheckForUnexpectedParametersInCredentialOffer_UnitTest extends AbstractVciUnitTest {

	private CheckForUnexpectedParametersInCredentialOffer cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckForUnexpectedParametersInCredentialOffer();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	public void testEvaluate_noWarningWhenNoUnknownProperties() {
		String json = """
			{
			  "credential_issuer": "https://credential-issuer.example.com",
			  "credential_configuration_ids": ["UniversityDegreeCredential"],
			  "grants": {
			    "authorization_code": {
			      "issuer_state": "eyJ..."
			    }
			  }
			}
			""";
		putCredentialOffer(json);
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_unknownTopLevelProperty() {
		String json = """
			{
			  "credential_issuer": "https://credential-issuer.example.com",
			  "credential_configuration_ids": ["UniversityDegreeCredential"],
			  "unexpected_field": "boom"
			}
			""";
		putCredentialOffer(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertUnknownPropertyAtPath(data, "$.unexpected_field");
	}

	@Test
	public void testEvaluate_structuralErrorsDoNotWarn() {
		// Missing required credential_configuration_ids is structural, not unknown property
		String json = """
			{
			  "credential_issuer": "https://credential-issuer.example.com"
			}
			""";
		putCredentialOffer(json);
		assertDoesNotThrow(() -> cond.execute(env));
	}

	private void putCredentialOffer(String json) {
		JsonObject offer = JsonParser.parseString(json).getAsJsonObject();
		JsonObject vci = new JsonObject();
		vci.add("credential_offer", offer);
		env.putObject("vci", vci);
	}
}
