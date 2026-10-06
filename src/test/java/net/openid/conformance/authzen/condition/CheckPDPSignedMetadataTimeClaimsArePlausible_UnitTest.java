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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class CheckPDPSignedMetadataTimeClaimsArePlausible_UnitTest {

	private static final long NOW = System.currentTimeMillis() / 1000L;
	private static final long SECONDS_PER_YEAR = 365L * 24 * 60 * 60;

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckPDPSignedMetadataTimeClaimsArePlausible cond;

	@BeforeEach
	public void setUp() {
		cond = new CheckPDPSignedMetadataTimeClaimsArePlausible();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putClaims(JsonObject claims) {
		JsonObject signedMetadata = new JsonObject();
		signedMetadata.add("claims", claims);
		env.putObject("pdp_signed_metadata", signedMetadata);
	}

	private JsonObject claims(long iat, long nbf, long exp) {
		JsonObject claims = new JsonObject();
		claims.addProperty("iat", iat);
		claims.addProperty("nbf", nbf);
		claims.addProperty("exp", exp);
		return claims;
	}

	@Test
	public void noTimeClaims_succeeds() {
		putClaims(new JsonObject());
		cond.execute(env);
	}

	@Test
	public void recentClaims_succeed() {
		putClaims(claims(NOW - 60, NOW - 60, NOW + 3600));
		cond.execute(env);
	}

	@Test
	public void iatBefore2012_fails() {
		putClaims(claims(1_000_000_000L, NOW - 60, NOW + 3600));
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("iat claim is before 2012-01-01"));
	}

	@Test
	public void nbfBefore2012_fails() {
		putClaims(claims(NOW - 60, 0L, NOW + 3600));
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("nbf claim is before 2012-01-01"));
	}

	@Test
	public void longLivedMetadataIssuedIn2023_succeeds() {
		// 2023-01-01T00:00:00Z: old, but a plausible issue time for long-lived signed metadata.
		putClaims(claims(1_672_531_200L, 1_672_531_200L, NOW + 3600));
		cond.execute(env);
	}

	@Test
	public void expInMilliseconds_fails() {
		putClaims(claims(NOW - 60, NOW - 60, (NOW + 3600) * 1000L));
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("more than 50 years in the future"));
	}

	@Test
	public void expJustUnder50Years_succeeds() {
		putClaims(claims(NOW - 60, NOW - 60, NOW + 49 * SECONDS_PER_YEAR));
		cond.execute(env);
	}
}
