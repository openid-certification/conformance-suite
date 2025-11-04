package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.condition.AbstractVciUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateDCQLQuery_UnitTest extends AbstractVciUnitTest {

	private ValidateDCQLQuery cond;

	@Mock
	private TestInstanceEventLog eventLog;

	private Environment env;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateDCQLQuery();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	public void testEvaluate_validDcqlWithVcSdJwt() {
		JsonObject dcql = JsonParser.parseString("{"
			+ "\"credentials\": ["
			+ "  {"
			+ "    \"id\": \"credential_1\","
			+ "    \"format\": \"dc+sd-jwt\","
			+ "    \"meta\": {"
			+ "      \"vct_values\": [\"https://example.com/identity_credential\"]"
			+ "    },"
			+ "    \"claims\": ["
			+ "      {"
			+ "        \"path\": [\"given_name\"]"
			+ "      },"
			+ "      {"
			+ "        \"path\": [\"family_name\"]"
			+ "      }"
			+ "    ]"
			+ "  }"
			+ "]"
			+ "}").getAsJsonObject();

		env.putObject("client", "dcql", dcql);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_validDcqlWithMdoc() {
		JsonObject dcql = JsonParser.parseString("{"
			+ "\"credentials\": ["
			+ "  {"
			+ "    \"id\": \"credential_1\","
			+ "    \"format\": \"mso_mdoc\","
			+ "    \"meta\": {"
			+ "      \"doctype_value\": \"org.iso.18013.5.1.mDL\""
			+ "    },"
			+ "    \"claims\": ["
			+ "      {"
			+ "        \"path\": [\"org.iso.18013.5.1\", \"given_name\"]"
			+ "      },"
			+ "      {"
			+ "        \"path\": [\"org.iso.18013.5.1\", \"family_name\"]"
			+ "      }"
			+ "    ]"
			+ "  }"
			+ "]"
			+ "}").getAsJsonObject();

		env.putObject("client", "dcql", dcql);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_validDcqlWithCredentialSets() {
		JsonObject dcql = JsonParser.parseString("{"
			+ "\"credentials\": ["
			+ "  {"
			+ "    \"id\": \"credential_1\","
			+ "    \"format\": \"dc+sd-jwt\","
			+ "    \"meta\": {"
			+ "      \"vct_values\": [\"https://example.com/identity_credential\"]"
			+ "    },"
			+ "    \"claims\": ["
			+ "      {"
			+ "        \"id\": \"claim_1\","
			+ "        \"path\": [\"given_name\"]"
			+ "      },"
			+ "      {"
			+ "        \"id\": \"claim_2\","
			+ "        \"path\": [\"family_name\"]"
			+ "      }"
			+ "    ],"
			+ "    \"claim_sets\": ["
			+ "      [\"claim_1\", \"claim_2\"]"
			+ "    ]"
			+ "  }"
			+ "],"
			+ "\"credential_sets\": ["
			+ "  {"
			+ "    \"options\": [[\"credential_1\"]],"
			+ "    \"required\": true,"
			+ "    \"purpose\": \"Identity verification\""
			+ "  }"
			+ "]"
			+ "}").getAsJsonObject();

		env.putObject("client", "dcql", dcql);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingDcql() {
		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingRequiredCredentials() {
		JsonObject dcql = JsonParser.parseString("{"
			+ "\"credential_sets\": []"
			+ "}").getAsJsonObject();

		env.putObject("client", "dcql", dcql);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsExpectedError(data, "$.credentials", "required property 'credentials' not found");
	}

	@Test
	public void testEvaluate_missingRequiredFormat() {
		JsonObject dcql = JsonParser.parseString("{"
			+ "\"credentials\": ["
			+ "  {"
			+ "    \"id\": \"credential_1\","
			+ "    \"meta\": {"
			+ "      \"vct_values\": [\"https://example.com/identity_credential\"]"
			+ "    }"
			+ "  }"
			+ "]"
			+ "}").getAsJsonObject();

		env.putObject("client", "dcql", dcql);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsExpectedError(data, "$.credentials[0].format", "required property 'format' not found");
	}

	@Test
	public void testEvaluate_missingRequiredMeta() {
		JsonObject dcql = JsonParser.parseString("{"
			+ "\"credentials\": ["
			+ "  {"
			+ "    \"id\": \"credential_1\","
			+ "    \"format\": \"dc+sd-jwt\""
			+ "  }"
			+ "]"
			+ "}").getAsJsonObject();

		env.putObject("client", "dcql", dcql);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsExpectedError(data, "$.credentials[0].meta", "required property 'meta' not found");
	}

	@Test
	public void testEvaluate_invalidFormatValue() {
		JsonObject dcql = JsonParser.parseString("{"
			+ "\"credentials\": ["
			+ "  {"
			+ "    \"id\": \"credential_1\","
			+ "    \"format\": \"invalid-format\","
			+ "    \"meta\": {}"
			+ "  }"
			+ "]"
			+ "}").getAsJsonObject();

		env.putObject("client", "dcql", dcql);

		// This should throw an error - just verify the validation fails
		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_invalidMdocClaimPath() {
		JsonObject dcql = JsonParser.parseString("{"
			+ "\"credentials\": ["
			+ "  {"
			+ "    \"id\": \"credential_1\","
			+ "    \"format\": \"mso_mdoc\","
			+ "    \"meta\": {"
			+ "      \"doctype_value\": \"org.iso.18013.5.1.mDL\""
			+ "    },"
			+ "    \"claims\": ["
			+ "      {"
			+ "        \"path\": [\"only_one_element\"]"
			+ "      }"
			+ "    ]"
			+ "  }"
			+ "]"
			+ "}").getAsJsonObject();

		env.putObject("client", "dcql", dcql);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		// mso_mdoc claims must have exactly 2 elements in the path array
		assertContainsExpectedError(data, "$.credentials[0].claims[0].path", "must have at least 2 items but found 1");
	}

	@Test
	public void testEvaluate_missingRequiredPathInClaim() {
		JsonObject dcql = JsonParser.parseString("{"
			+ "\"credentials\": ["
			+ "  {"
			+ "    \"id\": \"credential_1\","
			+ "    \"format\": \"dc+sd-jwt\","
			+ "    \"meta\": {"
			+ "      \"vct_values\": [\"https://example.com/identity_credential\"]"
			+ "    },"
			+ "    \"claims\": ["
			+ "      {"
			+ "        \"values\": [\"test\"]"
			+ "      }"
			+ "    ]"
			+ "  }"
			+ "]"
			+ "}").getAsJsonObject();

		env.putObject("client", "dcql", dcql);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsExpectedError(data, "$.credentials[0].claims[0].path", "required property 'path' not found");
	}

	@Test
	public void testEvaluate_missingRequiredOptionsInCredentialSet() {
		JsonObject dcql = JsonParser.parseString("{"
			+ "\"credentials\": ["
			+ "  {"
			+ "    \"id\": \"credential_1\","
			+ "    \"format\": \"dc+sd-jwt\","
			+ "    \"meta\": {}"
			+ "  }"
			+ "],"
			+ "\"credential_sets\": ["
			+ "  {"
			+ "    \"required\": true,"
			+ "    \"purpose\": \"Identity verification\""
			+ "  }"
			+ "]"
			+ "}").getAsJsonObject();

		env.putObject("client", "dcql", dcql);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsExpectedError(data, "$.credential_sets[0].options", "required property 'options' not found");
	}
}
