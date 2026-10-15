package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExtractPDPSignedMetadataValues_UnitTest {

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ExtractPDPSignedMetadataValues cond;

	@BeforeEach
	public void setUp() {
		cond = new ExtractPDPSignedMetadataValues();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putClaims(String claims) {
		JsonObject signed = new JsonObject();
		signed.add("claims", JsonParser.parseString(claims));
		env.putObject("pdp_signed_metadata", signed);
	}

	@Test
	public void metadataValues_areKeptAndJwtClaimsDropped() {
		putClaims("""
			{ "iss": "https://pdp.example.com", "iat": 1760000000, "exp": 1860000000, "nbf": 1760000000,
			  "aud": "pep", "jti": "1", "sub": "pdp",
			  "policy_decision_point": "https://pdp.example.com",
			  "access_evaluation_endpoint": "https://pdp.example.com/access/v1/evaluation",
			  "capabilities": [] }""");
		cond.execute(env);
		JsonObject values = env.getObject("pdp_signed_metadata_values");
		assertEquals(3, values.size());
		assertEquals("https://pdp.example.com", OIDFJSON.getString(values.get("policy_decision_point")));
		assertEquals("https://pdp.example.com/access/v1/evaluation", OIDFJSON.getString(values.get("access_evaluation_endpoint")));
		assertEquals(0, values.getAsJsonArray("capabilities").size());
	}

	@Test
	public void valuesAreCopies() {
		putClaims("""
			{ "iss": "https://pdp.example.com", "capabilities": [ "urn:ietf:params:authzen:x" ] }""");
		cond.execute(env);
		env.getObject("pdp_signed_metadata_values").getAsJsonArray("capabilities").add("changed");
		assertEquals(1, env.getElementFromObject("pdp_signed_metadata", "claims.capabilities").getAsJsonArray().size());
	}

	@Test
	public void onlyJwtClaims_givesNoValues() {
		putClaims("""
			{ "iss": "https://pdp.example.com", "iat": 1760000000 }""");
		cond.execute(env);
		assertEquals(0, env.getObject("pdp_signed_metadata_values").size());
	}

	@Test
	public void missingClaims_fails() {
		env.putObject("pdp_signed_metadata", new JsonObject());
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
