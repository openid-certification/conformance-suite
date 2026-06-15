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
public class VCIBatchStatusReferenceChecks_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIEnsureBatchStatusReferencesAreDistinct distinctCond;
	private VCIEnsureBatchStatusListIndicesAreUnpredictable unpredictableCond;
	private VCIWarnBatchStatusListUrisProvideHerdPrivacy herdCond;

	@BeforeEach
	public void setUp() {
		distinctCond = new VCIEnsureBatchStatusReferencesAreDistinct();
		distinctCond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		unpredictableCond = new VCIEnsureBatchStatusListIndicesAreUnpredictable();
		unpredictableCond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		herdCond = new VCIWarnBatchStatusListUrisProvideHerdPrivacy();
		herdCond.setProperties("UNIT-TEST", eventLog, ConditionResult.WARNING);
	}

	/** An SD-JWT capture carrying a status_list reference. */
	private JsonObject capture(String uri, Long idx) {
		JsonObject statusList = new JsonObject();
		if (uri != null) {
			statusList.addProperty("uri", uri);
		}
		if (idx != null) {
			statusList.addProperty("idx", idx);
		}
		JsonObject status = new JsonObject();
		status.add("status_list", statusList);
		JsonObject claims = new JsonObject();
		claims.add("status", status);
		JsonObject credential = new JsonObject();
		credential.add("claims", claims);
		JsonObject sdjwt = new JsonObject();
		sdjwt.add("credential", credential);
		JsonObject c = new JsonObject();
		c.addProperty("format", "sd_jwt_vc");
		c.add("sdjwt", sdjwt);
		return c;
	}

	/**
	 * A real mdoc capture (mDL) whose MSO carries a Token Status List reference (uri + idx), so the
	 * checks' mdoc extraction path (parse MSO -> RevocationStatus.StatusList) is exercised.
	 */
	private JsonObject mdocCapture(String uri, long idx) throws Exception {
		String deviceKey = new ECKeyGenerator(Curve.P_256).generate().toJSONString();
		String mdocB64Url = VciMdocUtils.createMdocCredential(
			deviceKey, DrivingLicense.MDL_DOCTYPE, null, null, uri, idx);
		byte[] issuerSigned = new Base64URL(mdocB64Url).decode();
		JsonObject c = new JsonObject();
		c.addProperty("format", "mso_mdoc");
		c.addProperty("mdoc_credential_cbor", Base64.getEncoder().encodeToString(issuerSigned));
		return c;
	}

	/** An SD-JWT capture with no status claim. */
	private JsonObject captureNoStatus() {
		JsonObject credential = new JsonObject();
		credential.add("claims", new JsonObject());
		JsonObject sdjwt = new JsonObject();
		sdjwt.add("credential", credential);
		JsonObject c = new JsonObject();
		c.addProperty("format", "sd_jwt_vc");
		c.add("sdjwt", sdjwt);
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

	// --- distinct references ------------------------------------------------------------------

	@Test
	public void distinct_allDistinct_passes() {
		put(capture("https://issuer/sl/1", 5L), capture("https://issuer/sl/1", 12L), capture("https://issuer/sl/1", 99L));
		distinctCond.execute(env);
	}

	@Test
	public void distinct_duplicateTuple_fails() {
		put(capture("https://issuer/sl/1", 5L), capture("https://issuer/sl/1", 5L));
		assertThrows(ConditionError.class, () -> distinctCond.execute(env));
	}

	@Test
	public void distinct_sameIdxDifferentUri_passes() {
		put(capture("https://issuer/sl/1", 5L), capture("https://issuer/sl/2", 5L));
		distinctCond.execute(env);
	}

	@Test
	public void distinct_fewerThanTwoWithStatus_skips() {
		put(capture("https://issuer/sl/1", 5L), captureNoStatus());
		distinctCond.execute(env);
	}

	// --- unpredictable indices ----------------------------------------------------------------

	@Test
	public void unpredictable_consecutiveIndices_fails() {
		put(capture("u", 1L), capture("u", 2L), capture("u", 3L));
		assertThrows(ConditionError.class, () -> unpredictableCond.execute(env));
	}

	@Test
	public void unpredictable_arithmeticSequence_fails() {
		put(capture("u", 10L), capture("u", 20L), capture("u", 30L));
		assertThrows(ConditionError.class, () -> unpredictableCond.execute(env));
	}

	@Test
	public void unpredictable_randomIndices_passes() {
		put(capture("u", 5L), capture("u", 913L), capture("u", 27L));
		unpredictableCond.execute(env);
	}

	@Test
	public void unpredictable_onlyTwoIndices_skips() {
		put(capture("u", 1L), capture("u", 2L));
		unpredictableCond.execute(env);
	}

	@Test
	public void unpredictable_noUriGroupReachesThree_skips() {
		put(capture("u1", 1L), capture("u1", 2L), capture("u2", 100L));
		unpredictableCond.execute(env);
	}

	// --- herd privacy (URIs) ------------------------------------------------------------------

	@Test
	public void herd_uniqueUriPerCredential_fails() {
		put(capture("https://issuer/sl/1", 5L), capture("https://issuer/sl/2", 8L), capture("https://issuer/sl/3", 2L));
		assertThrows(ConditionError.class, () -> herdCond.execute(env));
	}

	@Test
	public void herd_sharedUri_passes() {
		put(capture("https://issuer/sl/1", 5L), capture("https://issuer/sl/1", 8L), capture("https://issuer/sl/1", 2L));
		herdCond.execute(env);
	}

	@Test
	public void herd_fewerThanThree_skips() {
		put(capture("https://issuer/sl/1", 5L), capture("https://issuer/sl/2", 8L));
		herdCond.execute(env);
	}

	// --- mdoc status references (MSO RevocationStatus.StatusList extraction path) ----------------

	@Test
	public void mdoc_distinct_allDistinct_passes() throws Exception {
		put(mdocCapture("https://issuer/sl/1", 5L), mdocCapture("https://issuer/sl/1", 12L), mdocCapture("https://issuer/sl/1", 99L));
		distinctCond.execute(env);
	}

	@Test
	public void mdoc_distinct_duplicateTuple_fails() throws Exception {
		put(mdocCapture("https://issuer/sl/1", 5L), mdocCapture("https://issuer/sl/1", 5L));
		assertThrows(ConditionError.class, () -> distinctCond.execute(env));
	}

	@Test
	public void mdoc_unpredictable_arithmeticSequence_fails() throws Exception {
		put(mdocCapture("u", 10L), mdocCapture("u", 20L), mdocCapture("u", 30L));
		assertThrows(ConditionError.class, () -> unpredictableCond.execute(env));
	}

	@Test
	public void mdoc_unpredictable_randomIndices_passes() throws Exception {
		put(mdocCapture("u", 5L), mdocCapture("u", 913L), mdocCapture("u", 27L));
		unpredictableCond.execute(env);
	}

	@Test
	public void mdoc_herd_uniqueUriPerCredential_fails() throws Exception {
		put(mdocCapture("https://issuer/sl/1", 5L), mdocCapture("https://issuer/sl/2", 8L), mdocCapture("https://issuer/sl/3", 2L));
		assertThrows(ConditionError.class, () -> herdCond.execute(env));
	}

	@Test
	public void mdoc_herd_sharedUri_passes() throws Exception {
		put(mdocCapture("https://issuer/sl/1", 5L), mdocCapture("https://issuer/sl/1", 8L), mdocCapture("https://issuer/sl/1", 2L));
		herdCond.execute(env);
	}
}
