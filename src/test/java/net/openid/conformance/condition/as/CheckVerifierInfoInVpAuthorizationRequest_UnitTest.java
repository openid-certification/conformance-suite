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
public class CheckVerifierInfoInVpAuthorizationRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckVerifierInfoInVpAuthorizationRequest cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckVerifierInfoInVpAuthorizationRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noVerifierInfo() {
		JsonObject authParams = JsonParser.parseString("{\"client_id\": \"test\"}").getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, authParams);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_validVerifierInfoWithStringData() {
		JsonObject authParams = JsonParser.parseString(
			"{\"verifier_info\": {\"format\": \"jwt\", \"data\": \"eyJhbGciOiJSUzI1NiJ9\"}}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, authParams);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_validVerifierInfoWithObjectData() {
		JsonObject authParams = JsonParser.parseString(
			"{\"verifier_info\": {\"format\": \"json\", \"data\": {\"name\": \"verifier\"}}}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, authParams);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_validVerifierInfoWithCredentialIds() {
		JsonObject authParams = JsonParser.parseString(
			"{\"verifier_info\": {\"format\": \"jwt\", \"data\": \"eyJ\", \"credential_ids\": [\"cred1\", \"cred2\"]}}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, authParams);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_verifierInfoNotObject() {
		JsonObject authParams = JsonParser.parseString(
			"{\"verifier_info\": \"not_an_object\"}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, authParams);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingFormat() {
		JsonObject authParams = JsonParser.parseString(
			"{\"verifier_info\": {\"data\": \"eyJ\"}}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, authParams);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_formatNotString() {
		JsonObject authParams = JsonParser.parseString(
			"{\"verifier_info\": {\"format\": 123, \"data\": \"eyJ\"}}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, authParams);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingData() {
		JsonObject authParams = JsonParser.parseString(
			"{\"verifier_info\": {\"format\": \"jwt\"}}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, authParams);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_dataNotStringOrObject() {
		JsonObject authParams = JsonParser.parseString(
			"{\"verifier_info\": {\"format\": \"jwt\", \"data\": 123}}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, authParams);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_dataIsArray() {
		JsonObject authParams = JsonParser.parseString(
			"{\"verifier_info\": {\"format\": \"jwt\", \"data\": [1, 2]}}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, authParams);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_credentialIdsNotArray() {
		JsonObject authParams = JsonParser.parseString(
			"{\"verifier_info\": {\"format\": \"jwt\", \"data\": \"eyJ\", \"credential_ids\": \"not_array\"}}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, authParams);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_credentialIdsEmptyArray() {
		JsonObject authParams = JsonParser.parseString(
			"{\"verifier_info\": {\"format\": \"jwt\", \"data\": \"eyJ\", \"credential_ids\": []}}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, authParams);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_credentialIdsContainsNonString() {
		JsonObject authParams = JsonParser.parseString(
			"{\"verifier_info\": {\"format\": \"jwt\", \"data\": \"eyJ\", \"credential_ids\": [\"cred1\", 123]}}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, authParams);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

}
