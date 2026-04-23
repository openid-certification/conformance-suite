package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
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
public class ValidateRequestObjectAudForVP_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateRequestObjectAudForVP cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateRequestObjectAudForVP();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_audIsSelfIssuedString() {
		JsonObject reqObj = JsonParser.parseString(
			"{\"claims\": {\"aud\": \"https://self-issued.me/v2\"}}"
		).getAsJsonObject();
		env.putObject("authorization_request_object", reqObj);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_audIsVerifierBaseUrl() {
		String verifierUrl = "https://example.com/verifier";
		env.putString("base_url", verifierUrl);

		JsonObject reqObj = JsonParser.parseString(
			"{\"claims\": {\"aud\": \"" + verifierUrl + "\"}}"
		).getAsJsonObject();
		env.putObject("authorization_request_object", reqObj);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_audIsArrayContainingSelfIssued() {
		JsonObject reqObj = JsonParser.parseString(
			"{\"claims\": {\"aud\": [\"https://self-issued.me/v2\", \"https://other.example.com\"]}}"
		).getAsJsonObject();
		env.putObject("authorization_request_object", reqObj);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_audIsArrayContainingVerifierUrl() {
		String verifierUrl = "https://example.com/verifier";
		env.putString("base_url", verifierUrl);

		JsonObject reqObj = JsonParser.parseString(
			"{\"claims\": {\"aud\": [\"https://other.example.com\", \"" + verifierUrl + "\"]}}"
		).getAsJsonObject();
		env.putObject("authorization_request_object", reqObj);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_audIsUnrecognizedString() {
		env.putString("base_url", "https://example.com/verifier");

		JsonObject reqObj = JsonParser.parseString(
			"{\"claims\": {\"aud\": \"https://unknown.example.com\"}}"
		).getAsJsonObject();
		env.putObject("authorization_request_object", reqObj);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_audIsMissing() {
		JsonObject reqObj = JsonParser.parseString(
			"{\"claims\": {}}"
		).getAsJsonObject();
		env.putObject("authorization_request_object", reqObj);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_audIsArrayWithNoRecognizedValues() {
		env.putString("base_url", "https://example.com/verifier");

		JsonObject reqObj = JsonParser.parseString(
			"{\"claims\": {\"aud\": [\"https://unknown1.example.com\", \"https://unknown2.example.com\"]}}"
		).getAsJsonObject();
		env.putObject("authorization_request_object", reqObj);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

}
