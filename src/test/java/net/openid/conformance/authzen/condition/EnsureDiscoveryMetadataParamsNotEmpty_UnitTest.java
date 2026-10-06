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

@ExtendWith(MockitoExtension.class)
class EnsureDiscoveryMetadataParamsNotEmpty_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureDiscoveryMetadataParamsNotEmpty cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureDiscoveryMetadataParamsNotEmpty();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putMetadata(String json) {
		env.putObject("server", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void populatedMetadata_succeeds() {
		putMetadata("""
			{
				"policy_decision_point": "https://pdp.example.com",
				"access_evaluation_endpoint": "https://pdp.example.com/access/v1/evaluation",
				"capabilities": [ "urn:authzen:pdp:core:v1" ]
			}""");
		cond.execute(env);
	}

	@Test
	public void nullValue_fails() {
		putMetadata("""
			{ "policy_decision_point": "https://pdp.example.com", "search_subject_endpoint": null }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void emptyString_fails() {
		putMetadata("""
			{ "policy_decision_point": "https://pdp.example.com", "search_subject_endpoint": "" }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void emptyArray_fails() {
		putMetadata("""
			{ "policy_decision_point": "https://pdp.example.com", "capabilities": [] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void emptyObject_fails() {
		putMetadata("""
			{ "policy_decision_point": "https://pdp.example.com", "extension": {} }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
