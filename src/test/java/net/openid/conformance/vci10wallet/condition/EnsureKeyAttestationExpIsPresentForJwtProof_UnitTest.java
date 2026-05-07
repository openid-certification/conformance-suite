package net.openid.conformance.vci10wallet.condition;

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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsureKeyAttestationExpIsPresentForJwtProof_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureKeyAttestationExpIsPresentForJwtProof cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureKeyAttestationExpIsPresentForJwtProof();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putKeyAttestationClaims(Long exp) {
		JsonObject claims = new JsonObject();
		if (exp != null) {
			claims.addProperty("exp", exp);
		}
		JsonObject keyAttestationJwt = new JsonObject();
		keyAttestationJwt.add("claims", claims);
		env.putObject("vci", "key_attestation_jwt", keyAttestationJwt);
	}

	@Test
	public void passesWhenExpIsPresent() {
		putKeyAttestationClaims(Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsWhenExpIsMissing() {
		putKeyAttestationClaims(null);

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertEquals("invalid_proof", env.getString("vci", "credential_error_response.body.error"));
	}
}
