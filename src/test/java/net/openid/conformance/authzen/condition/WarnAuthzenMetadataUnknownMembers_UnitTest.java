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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class WarnAuthzenMetadataUnknownMembers_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnAuthzenMetadataUnknownMembers cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnAuthzenMetadataUnknownMembers();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void put(String json) {
		env.putObject("pdp", JsonParser.parseString(json).getAsJsonObject());
	}

	private void assertUnknown(String json) {
		put(json);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not define"), e.getMessage());
	}

	@Test
	public void registeredParameters_succeed() {
		put("""
			{ "policy_decision_point": "https://pdp.example.com", "access_evaluation_endpoint": "https://pdp.example.com/access/v1/evaluation", "access_evaluations_endpoint": "https://pdp.example.com/access/v1/evaluations", "search_subject_endpoint": "https://pdp.example.com/access/v1/search/subject", "search_resource_endpoint": "https://pdp.example.com/access/v1/search/resource", "search_action_endpoint": "https://pdp.example.com/access/v1/search/action", "capabilities": [], "signed_metadata": "eyJ..." }""");
		cond.execute(env);
	}

	@Test
	public void supportedCapabilities_warns() {
		assertUnknown("""
			{ "policy_decision_point": "https://pdp.example.com", "access_evaluation_endpoint": "https://pdp.example.com/access/v1/evaluation", "supported_capabilities": [] }""");
	}
}
