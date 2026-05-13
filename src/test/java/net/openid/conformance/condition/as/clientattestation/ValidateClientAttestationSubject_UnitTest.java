package net.openid.conformance.condition.as.clientattestation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
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
public class ValidateClientAttestationSubject_UnitTest {

	private static final String CLIENT_ID = "my_client_id";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateClientAttestationSubject cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateClientAttestationSubject();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void setupEnv(String attestationSub, String popIss) {
		env.putString("client", "client_id", CLIENT_ID);

		JsonObject attestationClaims = new JsonObject();
		if (attestationSub != null) {
			attestationClaims.addProperty("sub", attestationSub);
		}
		JsonObject attestation = new JsonObject();
		attestation.add("claims", attestationClaims);
		env.putObject("client_attestation_object", attestation);

		JsonObject popClaims = new JsonObject();
		if (popIss != null) {
			popClaims.addProperty("iss", popIss);
		}
		JsonObject pop = new JsonObject();
		pop.add("claims", popClaims);
		env.putObject("client_attestation_pop_object", pop);
	}

	@Test
	public void testEvaluate_subMatchesPopIssPasses() {
		setupEnv(CLIENT_ID, CLIENT_ID);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_subDiffersFromPopIssFails() {
		setupEnv(CLIENT_ID, "attacker_client_id");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_subMatchesPopIssButNotSelectedClientFails() {
		setupEnv("other_client_id", "other_client_id");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingSubFails() {
		setupEnv(null, CLIENT_ID);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingPopIssFails() {
		setupEnv(CLIENT_ID, null);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingClientAttestationObjectFails() {
		env.putString("client", "client_id", CLIENT_ID);
		JsonObject pop = new JsonObject();
		pop.add("claims", new JsonObject());
		env.putObject("client_attestation_pop_object", pop);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingClientAttestationPopObjectFails() {
		env.putString("client", "client_id", CLIENT_ID);
		JsonObject attestation = new JsonObject();
		JsonObject attestationClaims = new JsonObject();
		attestationClaims.addProperty("sub", CLIENT_ID);
		attestation.add("claims", attestationClaims);
		env.putObject("client_attestation_object", attestation);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingClientFails() {
		JsonObject attestationClaims = new JsonObject();
		attestationClaims.addProperty("sub", CLIENT_ID);
		JsonObject attestation = new JsonObject();
		attestation.add("claims", attestationClaims);
		env.putObject("client_attestation_object", attestation);

		JsonObject popClaims = new JsonObject();
		popClaims.addProperty("iss", CLIENT_ID);
		JsonObject pop = new JsonObject();
		pop.add("claims", popClaims);
		env.putObject("client_attestation_pop_object", pop);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
