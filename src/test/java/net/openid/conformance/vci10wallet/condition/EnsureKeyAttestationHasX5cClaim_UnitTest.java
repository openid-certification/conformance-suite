package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsureKeyAttestationHasX5cClaim_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureKeyAttestationHasX5cClaim cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureKeyAttestationHasX5cClaim();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putKeyAttestationJwt(JsonObject header) {
		JsonObject keyAttestationJwt = new JsonObject();
		keyAttestationJwt.add("header", header);
		env.putObject("vci", "key_attestation_jwt", keyAttestationJwt);
	}

	@Test
	public void passesWhenX5cIsPresent() {
		JsonObject header = new JsonObject();
		JsonArray x5c = new JsonArray();
		x5c.add("MIIB...");
		header.add("x5c", x5c);
		putKeyAttestationJwt(header);

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsAndSetsInvalidProofWhenX5cIsMissing() {
		putKeyAttestationJwt(new JsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertEquals("invalid_proof", env.getString("vci", "credential_error_response.body.error"));
	}

	@Test
	public void failsAndSetsInvalidProofWhenX5cIsEmpty() {
		JsonObject header = new JsonObject();
		header.add("x5c", new JsonArray());
		putKeyAttestationJwt(header);

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertEquals("invalid_proof", env.getString("vci", "credential_error_response.body.error"));
	}
}
