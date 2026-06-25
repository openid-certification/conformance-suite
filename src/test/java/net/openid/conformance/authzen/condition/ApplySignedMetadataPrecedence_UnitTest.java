package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
class ApplySignedMetadataPrecedence_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ApplySignedMetadataPrecedence cond;

	@BeforeEach
	public void setUp() {
		cond = new ApplySignedMetadataPrecedence();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putPdp(String json) {
		env.putObject("pdp", JsonParser.parseString(json).getAsJsonObject());
	}

	/**
	 * Mirror what {@link ExtractPDPSignedMetadata} stores: a {@code pdp_signed_metadata}
	 * object whose decoded JWT claims live under the {@code claims} member.
	 */
	private void putClaims(String json) {
		JsonObject signedMetadata = new JsonObject();
		signedMetadata.add("claims", JsonParser.parseString(json).getAsJsonObject());
		env.putObject("pdp_signed_metadata", signedMetadata);
	}

	@Test
	public void emptyClaims_isNoOp() {
		putPdp("{ \"access_evaluation_endpoint\": \"https://pdp.example.com/access/v1/evaluation\" }");
		putClaims("{ }");
		cond.execute(env);
		assertEquals("https://pdp.example.com/access/v1/evaluation",
			OIDFJSON.getString(env.getElementFromObject("pdp", "access_evaluation_endpoint")));
	}

	@Test
	public void signedValuesOverridePlainValues() {
		putPdp("{ \"access_evaluation_endpoint\": \"https://plain.example.com/eval\" }");
		putClaims("{ \"iss\": \"https://pdp.example.com\", \"access_evaluation_endpoint\": \"https://signed.example.com/eval\" }");
		cond.execute(env);
		assertEquals("https://signed.example.com/eval",
			OIDFJSON.getString(env.getElementFromObject("pdp", "access_evaluation_endpoint")));
	}

	@Test
	public void signedOnlyValuesAreAdded() {
		putPdp("{ \"access_evaluation_endpoint\": \"https://pdp.example.com/eval\" }");
		putClaims("{ \"iss\": \"https://pdp.example.com\", \"access_evaluations_endpoint\": \"https://pdp.example.com/evals\" }");
		cond.execute(env);
		assertEquals("https://pdp.example.com/evals",
			OIDFJSON.getString(env.getElementFromObject("pdp", "access_evaluations_endpoint")));
	}

	@Test
	public void jwtRegisteredClaimsAreNotCopied() {
		putPdp("{ \"access_evaluation_endpoint\": \"https://pdp.example.com/eval\" }");
		putClaims("{ \"iss\": \"https://pdp.example.com\", \"iat\": 1700000000, \"exp\": 1700003600 }");
		cond.execute(env);
		JsonObject pdp = env.getObject("pdp");
		assertFalse(pdp.has("iss"));
		assertFalse(pdp.has("iat"));
		assertFalse(pdp.has("exp"));
	}
}
