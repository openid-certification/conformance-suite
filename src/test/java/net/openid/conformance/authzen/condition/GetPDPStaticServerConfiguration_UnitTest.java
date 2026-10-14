package net.openid.conformance.authzen.condition;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class GetPDPStaticServerConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private GetPDPStaticServerConfiguration cond;

	@BeforeEach
	public void setUp() {
		cond = new GetPDPStaticServerConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putConfig(String json) {
		env.putObject("config", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void identifierOnly_succeeds() {
		// Endpoint fields are optional: an empty one falls back to the default path.
		putConfig("""
			{ "pdp": { "policy_decision_point": "https://pdp.example.com" } }""");
		cond.execute(env);
		assertEquals("https://pdp.example.com", env.getString("pdp", "policy_decision_point"));
	}

	@Test
	public void identifierAndEndpoints_areCopied() {
		putConfig("""
			{ "pdp": { "policy_decision_point": "https://pdp.example.com",
			           "search_subject_endpoint": "https://pdp.example.com/search/subject" } }""");
		cond.execute(env);
		assertEquals("https://pdp.example.com/search/subject", env.getString("pdp", "search_subject_endpoint"));
	}

	@Test
	public void missingIdentifier_namesTheField() {
		putConfig("""
			{ "pdp": { "access_evaluation_endpoint": "https://pdp.example.com/access/v1/evaluation" } }""");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("'PDP Identifier' field is missing"), e.getMessage());
	}

	@Test
	public void missingSection_namesTheSection() {
		putConfig("""
			{ "alias": "x" }""");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("'AuthZEN' section is missing"), e.getMessage());
	}
}
