package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class RemoveClientAttestationChallenge_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private RemoveClientAttestationChallenge cond;

	@BeforeEach
	public void setUp() {
		cond = new RemoveClientAttestationChallenge();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void storedChallengeIsRemoved() {
		JsonObject vci = new JsonObject();
		vci.addProperty("attestation_challenge", "challenge-from-previous-server");
		vci.addProperty("other", "kept");
		env.putObject("vci", vci);

		cond.execute(env);

		assertThat(env.getString("vci", "attestation_challenge")).isNull();
		assertThat(env.getString("vci", "other")).isEqualTo("kept");
	}

	@Test
	public void missingChallengeIsNoOp() {
		env.putObject("vci", new JsonObject());

		cond.execute(env);

		assertThat(env.getString("vci", "attestation_challenge")).isNull();
	}

	@Test
	public void missingVciObjectIsNoOp() {
		cond.execute(env);

		assertThat(env.getObject("vci")).isNull();
	}
}
