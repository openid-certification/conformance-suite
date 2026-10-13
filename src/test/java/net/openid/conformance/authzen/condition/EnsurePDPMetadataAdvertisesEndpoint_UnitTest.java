package net.openid.conformance.authzen.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EnsurePDPMetadataAdvertisesEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsurePDPMetadataAdvertisesEndpoint condition(String parameter) {
		EnsurePDPMetadataAdvertisesEndpoint cond = new EnsurePDPMetadataAdvertisesEndpoint(parameter);
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		return cond;
	}

	private void putMetadata(String json) {
		env.putObject("pdp", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void advertisedEndpoint_succeeds() {
		putMetadata("""
			{ "policy_decision_point": "https://pdp.example.com",
			  "access_evaluation_endpoint": "https://pdp.example.com/access/v1/evaluation",
			  "search_subject_endpoint": "https://pdp.example.com/access/v1/search/subject" }""");
		condition("search_subject_endpoint").execute(env);
	}

	@Test
	public void missingEndpoint_fails() {
		putMetadata("""
			{ "policy_decision_point": "https://pdp.example.com",
			  "access_evaluation_endpoint": "https://pdp.example.com/access/v1/evaluation" }""");
		ConditionError e = assertThrows(ConditionError.class, () -> condition("search_action_endpoint").execute(env));
		assertTrue(e.getMessage().contains("does not support the Action Search API"), e.getMessage());
		assertTrue(e.getMessage().contains("'PDP server metadata location' to static"), e.getMessage());
	}

	@Test
	public void emptyEndpoint_fails() {
		putMetadata("""
			{ "policy_decision_point": "https://pdp.example.com",
			  "access_evaluation_endpoint": "https://pdp.example.com/access/v1/evaluation",
			  "access_evaluations_endpoint": "" }""");
		assertThrows(ConditionError.class, () -> condition("access_evaluations_endpoint").execute(env));
	}

	@Test
	public void unknownParameter_isRejected() {
		assertThrows(IllegalArgumentException.class, () -> new EnsurePDPMetadataAdvertisesEndpoint("search_endpoint"));
	}
}
