package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.condition.AbstractVciUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class ValidateVerifierInfo_UnitTest extends AbstractVciUnitTest {

	private ValidateVerifierInfo cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateVerifierInfo();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	public void testEvaluate_validSingleEntryWithStringData() {
		putVerifierInfo("""
			[
			  { "format": "registration_certificate_jwt", "data": "eyJhbGciOiJFUzI1NiJ9.payload.sig" }
			]
			""");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_validEntryWithObjectData() {
		putVerifierInfo("""
			[
			  { "format": "policy_attestation", "data": { "policy": "high-assurance" } }
			]
			""");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_validEntryWithCredentialIds() {
		putVerifierInfo("""
			[
			  {
			    "format": "registration_certificate_jwt",
			    "data": "eyJhbGciOiJFUzI1NiJ9.payload.sig",
			    "credential_ids": ["credential_1", "credential_2"]
			  }
			]
			""");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_validMultipleEntries() {
		putVerifierInfo("""
			[
			  { "format": "registration_certificate_jwt", "data": "eyJhbGciOiJFUzI1NiJ9.a.b" },
			  { "format": "policy_attestation", "data": { "level": 3 } }
			]
			""");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_emptyArrayIsInvalid() {
		putVerifierInfo("[]");
		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsExpectedError(data, "$.verifier_info", "must have at least 1 items but found 0");
	}

	@Test
	public void testEvaluate_missingFormatIsInvalid() {
		putVerifierInfo("""
			[ { "data": "eyJhbGciOiJFUzI1NiJ9.a.b" } ]
			""");
		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsExpectedError(data, "$.verifier_info[0].format", "required property 'format' not found");
	}

	@Test
	public void testEvaluate_missingDataIsInvalid() {
		putVerifierInfo("""
			[ { "format": "registration_certificate_jwt" } ]
			""");
		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsExpectedError(data, "$.verifier_info[0].data", "required property 'data' not found");
	}

	@Test
	public void testEvaluate_credentialIdsMustBeNonEmpty() {
		putVerifierInfo("""
			[
			  {
			    "format": "registration_certificate_jwt",
			    "data": "x",
			    "credential_ids": []
			  }
			]
			""");
		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsExpectedError(data, "$.verifier_info[0].credential_ids", "must have at least 1 items but found 0");
	}

	@Test
	public void testEvaluate_credentialIdsMustBeStrings() {
		putVerifierInfo("""
			[
			  {
			    "format": "registration_certificate_jwt",
			    "data": "x",
			    "credential_ids": [123]
			  }
			]
			""");
		assertValidationError(cond, env, eventLog);
	}

	@Test
	public void testEvaluate_topLevelMustBeArray() {
		putVerifierInfo("""
			{ "format": "x", "data": "y" }
			""");
		assertValidationError(cond, env, eventLog);
	}

	@Test
	public void testEvaluate_unknownPropertiesIgnoredByValidator() {
		// The structural validator filters out unknown-property errors;
		// CheckForUnexpectedParametersInVerifierInfo handles those as warnings.
		putVerifierInfo("""
			[
			  {
			    "format": "registration_certificate_jwt",
			    "data": "x",
			    "extension_field": "ignored here"
			  }
			]
			""");
		cond.execute(env);
	}

	private void putVerifierInfo(String json) {
		JsonElement verifierInfo = JsonParser.parseString(json);
		JsonObject wrapper = new JsonObject();
		wrapper.add(ExtractVerifierInfoFromClientConfiguration.WRAPPER_PROPERTY, verifierInfo);
		env.putObject(ExtractVerifierInfoFromClientConfiguration.ENV_KEY, wrapper);
	}
}
