package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
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
public class ValidateRequestObjectAudForVP_UnitTest {

	private static final String WALLET_ISSUER = "https://wallet.example.com/issuer";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateRequestObjectAudForVP cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateRequestObjectAudForVP();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		JsonObject server = new JsonObject();
		server.addProperty("issuer", WALLET_ISSUER);
		env.putObject("server", server);
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
	public void testEvaluate_audIsWalletIssuer() {
		JsonObject reqObj = JsonParser.parseString(
			"{\"claims\": {\"aud\": \"" + WALLET_ISSUER + "\"}}"
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
	public void testEvaluate_audIsArrayContainingWalletIssuer() {
		JsonObject reqObj = JsonParser.parseString(
			"{\"claims\": {\"aud\": [\"https://other.example.com\", \"" + WALLET_ISSUER + "\"]}}"
		).getAsJsonObject();
		env.putObject("authorization_request_object", reqObj);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_audIsBaseUrl() {
		// Regression test for #1762: the verifier's base URL is NOT a valid aud per OID4VP §5.8.
		env.putString("base_url", "https://example.com/verifier");

		JsonObject reqObj = JsonParser.parseString(
			"{\"claims\": {\"aud\": \"https://example.com/verifier\"}}"
		).getAsJsonObject();
		env.putObject("authorization_request_object", reqObj);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_audIsUnrecognizedString() {
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
		JsonObject reqObj = JsonParser.parseString(
			"{\"claims\": {\"aud\": [\"https://unknown1.example.com\", \"https://unknown2.example.com\"]}}"
		).getAsJsonObject();
		env.putObject("authorization_request_object", reqObj);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

}
