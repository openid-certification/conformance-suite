package net.openid.conformance.authzen.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckSignedPDPServerConfiguration_UnitTest {

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckSignedPDPServerConfiguration cond;

	@BeforeEach
	public void setUp() {
		cond = new CheckSignedPDPServerConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void put(String merged, String signedValues) {
		env.putObject("server", JsonParser.parseString(merged).getAsJsonObject());
		env.putObject("pdp_signed_metadata_values", JsonParser.parseString(signedValues).getAsJsonObject());
	}

	@Test
	public void signedHttpsEndpoints_succeed() {
		put("""
			{ "policy_decision_point": "https://pdp.example.com",
			  "access_evaluation_endpoint": "https://pdp.example.com/access/v1/evaluation" }""", """
			{ "access_evaluation_endpoint": "https://pdp.example.com/access/v1/evaluation" }""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void signedHttpEndpoint_fails() {
		put("""
			{ "policy_decision_point": "https://pdp.example.com",
			  "access_evaluation_endpoint": "https://pdp.example.com/access/v1/evaluation",
			  "search_subject_endpoint": "http://pdp.example.com/access/v1/search/subject" }""", """
			{ "search_subject_endpoint": "http://pdp.example.com/access/v1/search/subject" }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void publishedEndpoint_isNotCheckedAgain() {
		// CheckPublishedPDPMetadataEndpoints has already reported the published http endpoint.
		put("""
			{ "policy_decision_point": "https://pdp.example.com",
			  "access_evaluation_endpoint": "https://pdp.example.com/access/v1/evaluation",
			  "search_subject_endpoint": "http://pdp.example.com/access/v1/search/subject" }""", """
			{ "access_evaluation_endpoint": "https://pdp.example.com/access/v1/evaluation" }""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void publishedRequiredEndpoint_isNotCheckedAgain() {
		put("""
			{ "policy_decision_point": "https://pdp.example.com",
			  "access_evaluation_endpoint": "http://pdp.example.com/access/v1/evaluation",
			  "search_subject_endpoint": "https://pdp.example.com/access/v1/search/subject" }""", """
			{ "search_subject_endpoint": "https://pdp.example.com/access/v1/search/subject" }""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void missingRequiredEndpoint_fails() {
		put("""
			{ "policy_decision_point": "https://pdp.example.com" }""", """
			{ "search_subject_endpoint": "https://pdp.example.com/access/v1/search/subject" }""");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("access_evaluation_endpoint"), e.getMessage());
	}
}
