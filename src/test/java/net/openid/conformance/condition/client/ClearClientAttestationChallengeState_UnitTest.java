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
public class ClearClientAttestationChallengeState_UnitTest {

	private static final String FLAG = ExtractClientAttestationChallengeFromResponseHeader.CHALLENGE_ISSUED_BY_SERVER_FLAG;

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ClearClientAttestationChallengeState cond;

	@BeforeEach
	public void setUp() {
		cond = new ClearClientAttestationChallengeState();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void emptyEnv_isNoOp() {
		cond.execute(env);

		assertThat(env.getString(FLAG)).isNull();
		assertThat(env.getString("vci", "attestation_challenge")).isNull();
	}

	@Test
	public void clearsFlagOnly() {
		env.putString(FLAG, "true");

		cond.execute(env);

		assertThat(env.getString(FLAG)).isNull();
	}

	@Test
	public void clearsAttestationChallengeOnly() {
		JsonObject vci = new JsonObject();
		vci.addProperty("attestation_challenge", "client1-challenge");
		env.putObject("vci", vci);

		cond.execute(env);

		assertThat(env.getString("vci", "attestation_challenge")).isNull();
	}

	@Test
	public void clearsBothAndLeavesOtherVciFieldsIntact() {
		JsonObject vci = new JsonObject();
		vci.addProperty("attestation_challenge", "client1-challenge");
		vci.addProperty("credential_issuer_url", "https://issuer.example/");
		env.putObject("vci", vci);
		env.putString(FLAG, "true");

		cond.execute(env);

		assertThat(env.getString(FLAG)).isNull();
		assertThat(env.getString("vci", "attestation_challenge")).isNull();
		assertThat(env.getString("vci", "credential_issuer_url")).isEqualTo("https://issuer.example/");
	}
}
