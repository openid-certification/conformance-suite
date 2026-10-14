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
class WarnAuthzenCapabilitiesNotAuthzenUrns_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnAuthzenCapabilitiesNotAuthzenUrns cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnAuthzenCapabilitiesNotAuthzenUrns();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void put(String json) {
		env.putObject("server", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void noCapabilities_succeeds() {
		put("""
			{ "policy_decision_point": "https://pdp.example.com" }""");
		cond.execute(env);
	}

	@Test
	public void authzenUrns_succeed() {
		put("""
			{ "capabilities": [ "urn:ietf:params:authzen:search-pagination-sort", "URN:IETF:params:authzen:x" ] }""");
		cond.execute(env);
	}

	@Test
	public void emptyCapabilities_succeed() {
		put("""
			{ "capabilities": [] }""");
		cond.execute(env);
	}

	@Test
	public void plainName_warns() {
		put("""
			{ "capabilities": [ "pagination" ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void otherUrnNamespace_warns() {
		put("""
			{ "capabilities": [ "urn:example:authzen:pagination" ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void bareSubNamespace_warns() {
		put("""
			{ "capabilities": [ "urn:ietf:params:authzen:" ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
