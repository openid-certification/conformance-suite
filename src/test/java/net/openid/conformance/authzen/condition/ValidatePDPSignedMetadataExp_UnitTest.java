package net.openid.conformance.authzen.condition;

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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ValidatePDPSignedMetadataExp_UnitTest {

	private static final long NOW = System.currentTimeMillis() / 1000L;
	private static final long SECONDS_PER_YEAR = 365L * 24 * 60 * 60;

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidatePDPSignedMetadataExp cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidatePDPSignedMetadataExp();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putClaims(JsonObject claims) {
		JsonObject signedMetadata = new JsonObject();
		signedMetadata.add("claims", claims);
		env.putObject("pdp_signed_metadata", signedMetadata);
	}

	private void putExp(long exp) {
		JsonObject claims = new JsonObject();
		claims.addProperty("exp", exp);
		putClaims(claims);
	}

	@Test
	public void noExp_skips() {
		// exp is optional; absence is logged and accepted.
		putClaims(new JsonObject());
		cond.execute(env);
	}

	@Test
	public void futureExp_succeeds() {
		putExp(NOW + 3600);
		cond.execute(env);
	}

	@Test
	public void expiredExp_fails() {
		putExp(NOW - 3600);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void implausiblyFarFutureExp_fails() {
		// More than ~50 years in the future (likely a non-seconds value).
		putExp(NOW + 51 * SECONDS_PER_YEAR);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
