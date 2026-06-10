package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
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
public class VCIEnsureCredentialTimeClaimsNotLinkable_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIEnsureCredentialTimeClaimsNotLinkable cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new VCIEnsureCredentialTimeClaimsNotLinkable();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
	}

	/** Build an SD-JWT capture entry storing the whole sdjwt object. iat null => omit iat. */
	private JsonObject sdJwtCapture(Long iat, long responseTime, boolean fallback) {
		JsonObject entry = new JsonObject();
		entry.addProperty("format", "sd_jwt_vc");
		entry.addProperty("response_time", responseTime);
		entry.addProperty("response_time_is_fallback", fallback);

		JsonObject claims = new JsonObject();
		if (iat != null) {
			claims.addProperty("iat", iat);
		}
		JsonObject credential = new JsonObject();
		credential.add("claims", claims);
		JsonObject sdjwt = new JsonObject();
		sdjwt.add("credential", credential);
		entry.add("sdjwt", sdjwt);
		return entry;
	}

	private void setDecoded(JsonObject sdJwtEntry, JsonObject decoded) {
		sdJwtEntry.getAsJsonObject("sdjwt").add("decoded", decoded);
	}

	private JsonObject mdocCapture(String cborBase64, long responseTime) {
		JsonObject entry = new JsonObject();
		entry.addProperty("format", "mso_mdoc");
		entry.addProperty("response_time", responseTime);
		entry.addProperty("response_time_is_fallback", false);
		entry.addProperty("mdoc_credential_cbor", cborBase64);
		return entry;
	}

	/** Store the captures as the linkability_captures.list array. */
	private void put(JsonObject... entries) {
		JsonArray list = new JsonArray();
		for (JsonObject e : entries) {
			list.add(e);
		}
		JsonObject container = new JsonObject();
		container.add("list", list);
		env.putObject("linkability_captures", container);
	}

	@Test
	public void preciseTracking_fails() {
		put(sdJwtCapture(1000L, 1000, false), sdJwtCapture(1005L, 1005, false));
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void roundedToDay_identicalIat_passes() {
		put(sdJwtCapture(86400L, 1000, false), sdJwtCapture(86400L, 1005, false));
		cond.execute(env);
	}

	@Test
	public void randomized_largePositiveJump_passes() {
		put(sdJwtCapture(5000L, 1000, false), sdJwtCapture(9000L, 1005, false));
		cond.execute(env);
	}

	@Test
	public void randomized_negativeJump_passes() {
		put(sdJwtCapture(9000L, 1000, false), sdJwtCapture(5000L, 1005, false));
		cond.execute(env);
	}

	@Test
	public void identicalSameSecond_passes() {
		put(sdJwtCapture(1000L, 1000, false), sdJwtCapture(1000L, 1000, false));
		cond.execute(env);
	}

	@Test
	public void withinToleranceBoundary_fails() {
		// deltaIat 4, deltaResp 5 => |4-5| = 1 <= 2 and deltaIat > 0.
		put(sdJwtCapture(1000L, 1000, false), sdJwtCapture(1004L, 1005, false));
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void justOutsideTolerance_passes() {
		// deltaIat 8, deltaResp 5 => |8-5| = 3 > 2.
		put(sdJwtCapture(1000L, 1000, false), sdJwtCapture(1008L, 1005, false));
		cond.execute(env);
	}

	@Test
	public void fallbackReference_widensTolerance_fails() {
		// Same numbers as justOutsideTolerance, but the fallback reference widens tolerance to 5.
		put(sdJwtCapture(1000L, 1000, true), sdJwtCapture(1008L, 1005, true));
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void absentIat_skips() {
		put(sdJwtCapture(null, 1000, false), sdJwtCapture(1005L, 1005, false));
		cond.execute(env);
	}

	@Test
	public void fewerThanTwoCaptures_skips() {
		put(sdJwtCapture(1000L, 1000, false));
		cond.execute(env);
	}

	@Test
	public void noCapturesAtAll_skips() {
		cond.execute(env);
	}

	@Test
	public void firstAndLastCompared_acrossThreeCaptures_fails() {
		put(sdJwtCapture(1000L, 1000, false), sdJwtCapture(1003L, 1003, false), sdJwtCapture(1006L, 1006, false));
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void differentDataset_skipsEvenWhenTimingLooksPrecise() {
		JsonObject first = sdJwtCapture(1000L, 1000, false);
		JsonObject firstClaims = new JsonObject();
		firstClaims.addProperty("vct", "https://example.com/credential");
		firstClaims.addProperty("given_name", "John");
		setDecoded(first, firstClaims);

		JsonObject second = sdJwtCapture(1005L, 1005, false);
		JsonObject secondClaims = new JsonObject();
		secondClaims.addProperty("vct", "https://example.com/credential");
		secondClaims.addProperty("given_name", "Jane");
		setDecoded(second, secondClaims);

		// Precise timing would throw without the guard; the guard makes it skip (no throw).
		put(first, second);
		cond.execute(env);
	}

	@Test
	public void sameDataset_volatileClaimsIgnored_failsOnPreciseTiming() {
		JsonObject first = sdJwtCapture(1000L, 1000, false);
		JsonObject firstClaims = new JsonObject();
		firstClaims.addProperty("vct", "https://example.com/credential");
		firstClaims.addProperty("given_name", "John");
		firstClaims.addProperty("iat", 1000L);
		firstClaims.addProperty("cnf", "key-1");
		setDecoded(first, firstClaims);

		JsonObject second = sdJwtCapture(1005L, 1005, false);
		JsonObject secondClaims = new JsonObject();
		secondClaims.addProperty("vct", "https://example.com/credential");
		secondClaims.addProperty("given_name", "John");
		secondClaims.addProperty("iat", 1005L);
		secondClaims.addProperty("cnf", "key-2");
		setDecoded(second, secondClaims);

		put(first, second);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void mdocUndecodable_skipsGracefully() {
		// base64 of "hello" decodes fine but is not valid mdoc CBOR => issuance time unavailable => skip.
		put(mdocCapture("aGVsbG8=", 1000), mdocCapture("aGVsbG8=", 1005));
		cond.execute(env);
	}
}
