package net.openid.conformance.condition.as.clientattestation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AbstractValidateClientAttestationJwtTyp_UnitTest {

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateClientAttestationTyp attestationCond;

	private ValidateClientAttestationPopTyp popCond;

	@BeforeEach
	public void setUp() {
		attestationCond = new ValidateClientAttestationTyp();
		attestationCond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		popCond = new ValidateClientAttestationPopTyp();
		popCond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void putHeader(String envKey, String typ) {
		JsonObject header = new JsonObject();
		if (typ != null) {
			header.addProperty("typ", typ);
		}
		JsonObject jwt = new JsonObject();
		jwt.add("header", header);
		env.putObject(envKey, jwt);
	}

	@Test
	public void attestation_correctTyp_passes() {
		putHeader("client_attestation_object", "oauth-client-attestation+jwt");
		assertDoesNotThrow(() -> attestationCond.execute(env));
	}

	@Test
	public void attestation_missingTyp_fails() {
		putHeader("client_attestation_object", null);
		assertThrows(ConditionError.class, () -> attestationCond.execute(env));
	}

	@Test
	public void attestation_popTyp_fails() {
		putHeader("client_attestation_object", "oauth-client-attestation-pop+jwt");
		assertThrows(ConditionError.class, () -> attestationCond.execute(env));
	}

	@Test
	public void attestation_plainJwtTyp_fails() {
		putHeader("client_attestation_object", "JWT");
		assertThrows(ConditionError.class, () -> attestationCond.execute(env));
	}

	@Test
	public void attestation_applicationPrefixedTyp_fails() {
		putHeader("client_attestation_object", "application/oauth-client-attestation+jwt");
		assertThrows(ConditionError.class, () -> attestationCond.execute(env));
	}

	@Test
	public void attestation_nonStringTyp_fails() {
		putHeader("client_attestation_object", null);
		env.getObject("client_attestation_object").getAsJsonObject("header").addProperty("typ", 5);
		assertThrows(ConditionError.class, () -> attestationCond.execute(env));
	}

	@Test
	public void pop_correctTyp_passes() {
		putHeader("client_attestation_pop_object", "oauth-client-attestation-pop+jwt");
		assertDoesNotThrow(() -> popCond.execute(env));
	}

	@Test
	public void pop_attestationTyp_fails() {
		putHeader("client_attestation_pop_object", "oauth-client-attestation+jwt");
		assertThrows(ConditionError.class, () -> popCond.execute(env));
	}

	@Test
	public void pop_missingTyp_fails() {
		putHeader("client_attestation_pop_object", null);
		assertThrows(ConditionError.class, () -> popCond.execute(env));
	}
}
