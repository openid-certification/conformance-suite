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
class ValidateVerifiedClaimsResponseAgainstCustomSchemas_UnitTest {
	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateVerifiedClaimsResponseAgainstCustomSchemas cond;

	@BeforeEach
	void setUp() {
		cond = new ValidateVerifiedClaimsResponseAgainstCustomSchemas();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void runTest(String configuredSchemas) {
		env.putObjectFromJsonString("config", "{\"ekyc\": {\"response_schemas\": " + configuredSchemas + "}}");
		env.putObject("verified_claims_response", JsonParser.parseString("""
			{
			  "id_token": {
			    "claims": {"given_name": "Paula"},
			    "verification": {"trust_framework": "de_aml"}
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
			  "required": ["verified_claims"]
			}
			"""));
	}

	@Test
	public void testEvaluate_dataFailingCustomSchema() {
		assertThrows(ConditionError.class, () -> runTest("""
			{
			  "$schema": "https://json-schema.org/draft/2020-12/schema",
			  "type": "object",
			  "required": ["not_present"]
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
		assertTrue(e.getMessage().contains("eKYC Additional Response Validation Schemas"),
			() -> "unexpected message: " + e.getMessage());
	}

	@Test
	public void testEvaluate_schemaThatIsNotAnObjectIsAConfigError() {
		ConditionError e = assertThrows(ConditionError.class, () -> runTest("\"not-a-schema\""));
		assertTrue(e.getMessage().contains("eKYC Additional Response Validation Schemas"),
			() -> "unexpected message: " + e.getMessage());
	}

	@Test
	public void testEvaluate_malformedSchemaKeywordIsAConfigErrorNotACrash() {
		// An invalid "pattern" regex is thrown from schema building rather than reported as a
		// validation error; it must reach the tester as a configuration problem too.
		ConditionError e = assertThrows(ConditionError.class, () -> runTest("""
			{
			  "$schema": "https://json-schema.org/draft/2020-12/schema",
			  "type": "object",
			  "properties": {"verified_claims": {"type": "string", "pattern": "["}}
			}
			"""));
		assertTrue(e.getMessage().contains("eKYC Additional Response Validation Schemas"),
			() -> "unexpected message: " + e.getMessage());
	}
}
