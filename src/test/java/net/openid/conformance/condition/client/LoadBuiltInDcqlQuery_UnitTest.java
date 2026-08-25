package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class LoadBuiltInDcqlQuery_UnitTest {

	private LoadBuiltInDcqlQuery cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new LoadBuiltInDcqlQuery();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	public void testEvaluate_missingResource() {
		env.putString(LoadBuiltInDcqlQuery.RESOURCE_ENV_KEY, "/json/dcql/no-such-query.json");

		assertThrows(ConditionError.class, () -> cond.evaluate(env));
	}

	/**
	 * Every built-in query must be usable by every module in the wallet test plan, which means it
	 * has to satisfy the preconditions of the conditions that mutate it: at least two claims for
	 * {@link RemoveLastClaimFromDcqlQuery} and no credential_sets for
	 * {@link AbstractAddNonMatchingCredentialToDcqlQuery}. It must also use the credential format
	 * that its credential type implies, since the credential type is only selectable alongside
	 * that format.
	 */
	@ParameterizedTest
	@CsvSource({
		"/json/dcql/vp1final-wallet-eudi-pid.json, dc+sd-jwt",
		"/json/dcql/vp1final-wallet-mdl.json, mso_mdoc",
	})
	public void testEvaluate_builtInQueryIsUsableByEveryModule(String resource, String expectedFormat) {
		env.putString(LoadBuiltInDcqlQuery.RESOURCE_ENV_KEY, resource);

		cond.evaluate(env);

		JsonObject dcql = env.getObject("dcql_query");
		assertFalse(dcql.has("credential_sets"), "built-in queries must not contain credential_sets");

		JsonArray credentials = dcql.getAsJsonArray("credentials");
		assertEquals(1, credentials.size(), "built-in queries request exactly one credential");

		JsonObject credential = credentials.get(0).getAsJsonObject();
		assertEquals(expectedFormat, OIDFJSON.getString(credential.get("format")));
		assertTrue(credential.getAsJsonArray("claims").size() >= 2,
			"built-in queries must request at least 2 claims");
	}

	/** The built-in queries must pass the same schema validation as a configured query. */
	@ParameterizedTest
	@CsvSource({
		"/json/dcql/vp1final-wallet-eudi-pid.json",
		"/json/dcql/vp1final-wallet-mdl.json",
	})
	public void testEvaluate_builtInQueryIsSchemaValid(String resource) {
		env.putString(LoadBuiltInDcqlQuery.RESOURCE_ENV_KEY, resource);
		cond.evaluate(env);

		ValidateDCQLQuery validate = new ValidateDCQLQuery();
		validate.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		validate.evaluate(env);
	}
}
