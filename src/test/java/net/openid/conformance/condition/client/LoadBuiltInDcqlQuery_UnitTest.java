package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.as.ExtractDCQLQueryFromAuthorizationRequest;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.vp1finalwallet.VP1FinalWalletCredentialType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.stream.Stream;

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

	/** The credential types with a built-in query, so a newly added type is covered automatically. */
	private static Stream<VP1FinalWalletCredentialType> builtInTypes() {
		return Arrays.stream(VP1FinalWalletCredentialType.values())
			.filter(type -> type.getDcqlResource() != null);
	}

	/**
	 * The credential format each built-in query must request — the same fact the
	 * {@code @VariantNotApplicableWhen} rules on AbstractVP1FinalWalletTest encode. The exhaustive
	 * switch means adding a credential type without declaring its format here fails to compile.
	 */
	private static String expectedFormat(VP1FinalWalletCredentialType type) {
		return switch (type) {
			case EUDI_PID -> "dc+sd-jwt";
			case MDL, PHOTO_ID -> "mso_mdoc";
			case CUSTOM -> throw new IllegalArgumentException("custom has no built-in query");
		};
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
	@MethodSource("builtInTypes")
	public void testEvaluate_builtInQueryIsUsableByEveryModule(VP1FinalWalletCredentialType type) {
		env.putString(LoadBuiltInDcqlQuery.RESOURCE_ENV_KEY, type.getDcqlResource());

		cond.evaluate(env);

		JsonObject dcql = env.getObject(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY);
		assertFalse(dcql.has("credential_sets"), "built-in queries must not contain credential_sets");

		JsonArray credentials = dcql.getAsJsonArray("credentials");
		assertEquals(1, credentials.size(), "built-in queries request exactly one credential");

		JsonObject credential = credentials.get(0).getAsJsonObject();
		assertEquals(expectedFormat(type), OIDFJSON.getString(credential.get("format")));
		assertTrue(credential.getAsJsonArray("claims").size() >= 2,
			"built-in queries must request at least 2 claims");
	}

	/** The built-in queries must pass the same schema validation as a configured query. */
	@ParameterizedTest
	@MethodSource("builtInTypes")
	public void testEvaluate_builtInQueryIsSchemaValid(VP1FinalWalletCredentialType type) {
		env.putString(LoadBuiltInDcqlQuery.RESOURCE_ENV_KEY, type.getDcqlResource());
		cond.evaluate(env);

		ValidateDCQLQuery validate = new ValidateDCQLQuery();
		validate.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		validate.evaluate(env);
	}

	/**
	 * The all-mandatory-claims queries must load, be schema valid, use the format the credential
	 * type implies, and be a superset of the corresponding minimal query's claims (a wallet that
	 * passes the happy flow must have every claim this module requests available).
	 */
	@ParameterizedTest
	@MethodSource("builtInTypes")
	public void testEvaluate_allMandatoryClaimsQueryIsValidSupersetOfMinimalQuery(VP1FinalWalletCredentialType type) {
		env.putString(LoadBuiltInDcqlQuery.RESOURCE_ENV_KEY, type.getDcqlResource());
		cond.evaluate(env);
		JsonArray minimalClaims = env.getObject(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY)
			.getAsJsonArray("credentials").get(0).getAsJsonObject().getAsJsonArray("claims");

		env.putString(LoadBuiltInDcqlQuery.RESOURCE_ENV_KEY, type.getAllMandatoryClaimsDcqlResource());
		cond.evaluate(env);

		ValidateDCQLQuery validate = new ValidateDCQLQuery();
		validate.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		validate.evaluate(env);

		JsonObject credential = env.getObject(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY)
			.getAsJsonArray("credentials").get(0).getAsJsonObject();
		assertEquals(expectedFormat(type), OIDFJSON.getString(credential.get("format")));

		JsonArray allClaims = credential.getAsJsonArray("claims");
		assertTrue(allClaims.size() > minimalClaims.size(),
			"the all-mandatory query must request more claims than the minimal one");
		for (var minimalClaim : minimalClaims) {
			assertTrue(allClaims.contains(minimalClaim),
				"the all-mandatory query must include the minimal query's claim " + minimalClaim);
		}
	}
}
