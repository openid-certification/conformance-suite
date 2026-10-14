package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
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

@ExtendWith(MockitoExtension.class)
class EnsureMetadataCapabilitiesValid_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private static JsonObject json(String s) {
		return JsonParser.parseString(s).getAsJsonObject();
	}

	private void run(AbstractCondition cond) {
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		cond.execute(env);
	}

	private void fails(AbstractCondition cond) {
		assertThrows(ConditionError.class, () -> run(cond));
	}

	@Test
	public void absent_succeeds() {
		env.putObject("server", json("{ \"policy_decision_point\": \"https://pdp.example.com\" }"));
		run(new EnsureMetadataCapabilitiesValid());
	}

	@Test
	public void arrayOfStrings_succeeds() {
		env.putObject("server", json("{ \"capabilities\": [ \"urn:ietf:params:authzen:a\", \"urn:ietf:params:authzen:b\" ] }"));
		run(new EnsureMetadataCapabilitiesValid());
	}

	@Test
	public void notAnArray_fails() {
		env.putObject("server", json("{ \"capabilities\": \"urn:ietf:params:authzen:a\" }"));
		fails(new EnsureMetadataCapabilitiesValid());
	}

	@Test
	public void nonStringEntry_fails() {
		env.putObject("server", json("{ \"capabilities\": [ \"urn:ietf:params:authzen:a\", 7 ] }"));
		fails(new EnsureMetadataCapabilitiesValid());
	}
}
