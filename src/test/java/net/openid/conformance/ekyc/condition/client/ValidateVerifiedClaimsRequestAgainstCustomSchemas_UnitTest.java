package net.openid.conformance.ekyc.condition.client;

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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ValidateVerifiedClaimsRequestAgainstCustomSchemas_UnitTest {
	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateVerifiedClaimsRequestAgainstCustomSchemas cond;

	@BeforeEach
	void setUp() {
		cond = new ValidateVerifiedClaimsRequestAgainstCustomSchemas();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void runTest(String configuredSchemas) {
		env.putObjectFromJsonString("config", "{\"ekyc\": {\"request_schemas\": " + configuredSchemas + "}}");
		env.putObject("authorization_endpoint_request", JsonParser.parseString("""
			{
			  "claims": {
			    "id_token": {
			      "verified_claims": {
			        "claims": {"given_name": null},
			        "verification": {"trust_framework": {"value": "de_aml"}}
			      }
			    }
			  }
			}
			""").getAsJsonObject());
		cond.execute(env);
	}

	@Test
	public void testEvaluate_noError() {
		assertDoesNotThrow(() -> runTest("""
			{
			  "$schema": "https://json-schema.org/draft/2020-12/schema",
			  "type": "object",
			  "required": ["id_token"]
			}
			"""));
	}

	@Test
	public void testEvaluate_dataFailingCustomSchema() {
		assertThrows(ConditionError.class, () -> runTest("""
			{
			  "$schema": "https://json-schema.org/draft/2020-12/schema",
			  "type": "object",
			  "required": ["userinfo"]
			}
			"""));
	}

	@Test
	public void testEvaluate_schemaWithoutSchemaTagIsAConfigErrorNotACrash() {
		// A custom schema is user-supplied test configuration; a missing/unrecognizable $schema
		// must surface as a ConditionError pointing at the configuration field, not as an
		// uncaught SchemaException.
		ConditionError e = assertThrows(ConditionError.class, () -> runTest("""
			{
			  "type": "object"
			}
			"""));
		assertTrue(e.getMessage().contains("eKYC Additional Request Validation Schemas"),
			() -> "unexpected message: " + e.getMessage());
	}

	@Test
	public void testEvaluate_schemaThatIsNotAnObjectIsAConfigError() {
		ConditionError e = assertThrows(ConditionError.class, () -> runTest("\"not-a-schema\""));
		assertTrue(e.getMessage().contains("eKYC Additional Request Validation Schemas"),
			() -> "unexpected message: " + e.getMessage());
	}
}
