package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.Base64URL;
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
import org.multipaz.documenttype.knowntypes.DrivingLicense;
import org.multipaz.testapp.VciMdocUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VCIEnsureBatchTimeClaimsNotLinkable_UnitTest {

	// A realistic "now" that is not a UTC day boundary, and the day boundary below it.
	private static final long NOW = 1_700_000_123L;
	private static final long DAY_BOUNDARY = 1_699_920_000L; // 19675 * 86400
	private static final long OLD = NOW - 100_000L;          // identical-but-old, not a boundary

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIEnsureBatchTimeClaimsNotLinkable cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureBatchTimeClaimsNotLinkable();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
	}

	private JsonObject cap(Long iat, Long nbf, long responseTime) {
		JsonObject claims = new JsonObject();
		if (iat != null) {
			claims.addProperty("iat", iat);
		}
		if (nbf != null) {
			claims.addProperty("nbf", nbf);
		}
		JsonObject credential = new JsonObject();
		credential.add("claims", claims);
		JsonObject sdjwt = new JsonObject();
		sdjwt.add("credential", credential);
		JsonObject c = new JsonObject();
		c.addProperty("format", "sd_jwt_vc");
		c.add("sdjwt", sdjwt);
		c.addProperty("response_time", responseTime);
		c.addProperty("response_time_is_fallback", false);
		return c;
	}

	/** An SD-JWT capture carrying iat and exp (exp is an expiry claim, judged without a now anchor). */
	private JsonObject capWithExp(Long iat, long exp, long responseTime) {
		JsonObject c = cap(iat, null, responseTime);
		c.getAsJsonObject("sdjwt").getAsJsonObject("credential").getAsJsonObject("claims")
			.addProperty("exp", exp);
		return c;
	}

	private void put(JsonObject... captures) {
		JsonArray list = new JsonArray();
		for (JsonObject c : captures) {
			list.add(c);
		}
		JsonObject container = new JsonObject();
		container.add("list", list);
		env.putObject("linkability_captures", container);
	}

	@Test
	public void preciseSharedIat_fails() {
		// All credentials share an identical iat equal to the issuance instant (the response Date).
		put(cap(NOW, null, NOW), cap(NOW, null, NOW), cap(NOW, null, NOW));
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void roundedToDay_passes() {
		put(cap(DAY_BOUNDARY, null, NOW), cap(DAY_BOUNDARY, null, NOW));
		cond.execute(env);
	}

	@Test
	public void roundedToHour_justAfterBoundary_passes() {
		// An hour-rounding issuer issuing just after the hour produces a shared value near "now";
		// the hour boundary marks it as rounded, so the verdict must not depend on the time of day.
		long hourBoundary = (NOW / 3600) * 3600;
		put(cap(hourBoundary, null, hourBoundary + 60), cap(hourBoundary, null, hourBoundary + 60));
		cond.execute(env);
	}

	@Test
	public void randomizedSpread_passes() {
		put(cap(NOW - 50, null, NOW), cap(NOW - 9_000, null, NOW), cap(NOW - 300_000, null, NOW));
		cond.execute(env);
	}

	@Test
	public void identicalButNotNearNow_passes() {
		// A fixed/past value shared across the batch is low-entropy, not the precise issuance instant.
		put(cap(OLD, null, NOW), cap(OLD, null, NOW));
		cond.execute(env);
	}

	@Test
	public void preciseSharedNbf_fails() {
		// nbf is also an issuance-time claim and is checked even when iat is absent.
		put(cap(null, NOW, NOW), cap(null, NOW, NOW));
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void narrowlySpreadIatNearNow_fails() {
		// A precise issuer whose per-credential iat differs by a couple of seconds is a tight cluster
		// at the issuance instant, not meaningful randomization, so it must still fail.
		put(cap(NOW, null, NOW), cap(NOW + 1, null, NOW), cap(NOW + 2, null, NOW));
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void iatRoundedButExpPrecise_fails() {
		// iat rounded to the hour (passes on its own), but exp = precise now + ttl is a shared,
		// non-rounded value across the batch => exp is the linkability vector and must be caught.
		long hourBoundary = (NOW / 3600) * 3600;
		long preciseExp = NOW + 1_209_600L; // now + 14 days; inherits now's sub-hour offset (not aligned)
		put(capWithExp(hourBoundary, preciseExp, NOW), capWithExp(hourBoundary, preciseExp, NOW));
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void iatRoundedAndExpRounded_passes() {
		// Both iat and exp on hour boundaries (the emulated issuer's iat + whole-hour ttl shape).
		long hourBoundary = (NOW / 3600) * 3600;
		long roundedExp = hourBoundary + 1_209_600L; // 14 days = whole hours, stays hour-aligned
		put(capWithExp(hourBoundary, roundedExp, NOW), capWithExp(hourBoundary, roundedExp, NOW));
		cond.execute(env);
	}

	/** A real mdoc capture (mDL) whose MSO signed/validFrom is the given epoch second. */
	private JsonObject mdocCap(long signedEpoch, long responseTime) throws Exception {
		String deviceKey = new ECKeyGenerator(Curve.P_256).generate().toJSONString();
		String mdocB64Url = VciMdocUtils.createMdocCredential(
			deviceKey, DrivingLicense.MDL_DOCTYPE, null, signedEpoch);
		byte[] issuerSigned = new Base64URL(mdocB64Url).decode();
		JsonObject c = new JsonObject();
		c.addProperty("format", "mso_mdoc");
		c.addProperty("mdoc_credential_cbor", Base64.getEncoder().encodeToString(issuerSigned));
		c.addProperty("response_time", responseTime);
		c.addProperty("response_time_is_fallback", false);
		return c;
	}

	@Test
	public void mdocPreciseSharedSigned_fails() throws Exception {
		// An mdoc issuer that embeds the precise issuance instant gives every credential in the batch
		// the same near-now signed/validFrom => the heuristic must catch it (parity with SD-JWT iat).
		put(mdocCap(NOW, NOW), mdocCap(NOW, NOW));
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void mdocRoundedSharedSigned_passes() throws Exception {
		// Rounding signed/validFrom down to the hour (as the emulated issuer does) is the fix: the
		// shared value lands on an hour boundary and is treated as rounded, not a precise timestamp.
		long hourBoundary = (NOW / 3600) * 3600;
		put(mdocCap(hourBoundary, NOW), mdocCap(hourBoundary, NOW));
		cond.execute(env);
	}

	@Test
	public void fewerThanTwoCaptures_skips() {
		put(cap(NOW, null, NOW));
		cond.execute(env);
	}

	@Test
	public void noIssuanceClaim_skips() {
		put(cap(null, null, NOW), cap(null, null, NOW));
		cond.execute(env);
	}
}
