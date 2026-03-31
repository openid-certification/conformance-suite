package net.openid.conformance.condition.client;

import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.Tstr;
import org.multipaz.cbor.Uint;
import org.multipaz.cbor.Bstr;
import org.multipaz.cbor.Tagged;
import org.multipaz.cbor.CborMap;
import org.multipaz.cose.CoseSign1;
import org.multipaz.mdoc.mso.MobileSecurityObject;
import org.multipaz.revocation.RevocationStatus;
import org.multipaz.testapp.VciMdocUtils;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateMdocMsoRevocationMechanism_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateMdocMsoRevocationMechanism cond;

	private static final String DEVICE_KEY_JWK = """
		{
			"kty": "EC",
			"crv": "P-256",
			"x": "cwYyuS94hcOtcPlrMMtGtflCfbZUwz5Mf1Gfa2m0AM8",
			"y": "KB7sJkFQyB8jZHO9vmWS5LNECL4id3OJO9HX9ChNonA",
			"d": "7N8jd8HvUp3vHC7a-xitehRnYuyZLy3kqkxG7KmpfMY"
		}
		""";

	@BeforeEach
	public void setUp() {
		cond = new ValidateMdocMsoRevocationMechanism();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noStatusField() {
		// VciMdocUtils creates mdocs without a status field
		String mdocBase64Url = VciMdocUtils.createMdocCredential(
			DEVICE_KEY_JWK, "org.iso.18013.5.1.mDL", null);

		byte[] bytes = new Base64URL(mdocBase64Url).decode();
		env.putString("mdoc_credential_cbor", Base64.getEncoder().encodeToString(bytes));

		// Should succeed — status is optional
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_validStatusList() {
		// Build a status field with status_list mechanism
		RevocationStatus status = new RevocationStatus.StatusList(
			42, "https://example.com/status-list", null);

		byte[] mdocBytes = createMdocWithStatus(status.toDataItem());
		env.putString("mdoc_credential_cbor", Base64.getEncoder().encodeToString(mdocBytes));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_validIdentifierList() {
		// Build a status field with identifier_list mechanism
		byte[] id = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
		RevocationStatus status = new RevocationStatus.IdentifierList(
			new kotlinx.io.bytestring.ByteString(id, 0, id.length),
			"https://example.com/identifier-list", null);

		byte[] mdocBytes = createMdocWithStatus(status.toDataItem());
		env.putString("mdoc_credential_cbor", Base64.getEncoder().encodeToString(mdocBytes));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_unknownRevocationMechanism() {
		// Build a status field with an unrecognized mechanism
		Map<DataItem, DataItem> unknownStatusMap = new LinkedHashMap<>();
		unknownStatusMap.put(new Tstr("some_unknown_mechanism"),
			new CborMap(Map.of(new Tstr("foo"), new Tstr("bar")), false));
		DataItem unknownStatus = new CborMap(unknownStatusMap, false);

		byte[] mdocBytes = createMdocWithStatus(unknownStatus);
		env.putString("mdoc_credential_cbor", Base64.getEncoder().encodeToString(mdocBytes));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	/**
	 * Creates an mdoc IssuerSigned structure with a status field injected into the MSO.
	 *
	 * Approach: starts with a valid mdoc from VciMdocUtils, then decodes the CBOR structure
	 * (IssuerSigned → issuerAuth COSE_Sign1 → MSO payload), adds the "status" field to the
	 * MSO CBOR map, and reassembles the structure. The COSE_Sign1 signature becomes invalid
	 * since the payload changed, but the condition under test only parses the MSO — it does
	 * not verify the signature (that's ValidateMdocIssuerSignedSignature's job).
	 */
	private byte[] createMdocWithStatus(DataItem statusDataItem) {
		// Generate a valid mdoc to use as a starting point
		String mdocBase64Url = VciMdocUtils.createMdocCredential(
			DEVICE_KEY_JWK, "org.iso.18013.5.1.mDL", null);
		byte[] originalBytes = new Base64URL(mdocBase64Url).decode();

		// Parse IssuerSigned structure
		DataItem issuerSignedItem = Cbor.INSTANCE.decode(originalBytes);
		DataItem issuerAuthItem = issuerSignedItem.get("issuerAuth");
		CoseSign1 originalCose = issuerAuthItem.getAsCoseSign1();

		// Parse the MSO from the COSE_Sign1 payload
		byte[] payload = originalCose.getPayload();
		DataItem payloadItem = Cbor.INSTANCE.decode(payload);
		DataItem msoDataItem = payloadItem.getAsTaggedEncodedCbor();

		// Rebuild the MSO map with the status field added
		Map<DataItem, DataItem> msoMap = new LinkedHashMap<>(msoDataItem.getAsMap());
		msoMap.put(new Tstr("status"), statusDataItem);
		DataItem newMsoDataItem = new CborMap(msoMap, false);

		// Re-encode: MSO -> TaggedEncodedCBOR -> COSE_Sign1 payload
		byte[] newMsoBytes = Cbor.INSTANCE.encode(newMsoDataItem);
		byte[] newPayload = Cbor.INSTANCE.encode(
			new Tagged(Tagged.ENCODED_CBOR, new Bstr(newMsoBytes)));

		// Build new COSE_Sign1 with modified payload (signature is now invalid, but not checked)
		CoseSign1 newCose = new CoseSign1(
			originalCose.getProtectedHeaders(),
			originalCose.getUnprotectedHeaders(),
			originalCose.getSignature(),
			newPayload);

		// Rebuild IssuerSigned with the modified issuerAuth
		byte[] newIssuerAuth = Cbor.INSTANCE.encode(newCose.toDataItem());
		Map<DataItem, DataItem> issuerSignedMap = new LinkedHashMap<>(issuerSignedItem.getAsMap());
		issuerSignedMap.put(new Tstr("issuerAuth"),
			Cbor.INSTANCE.decode(newIssuerAuth));
		return Cbor.INSTANCE.encode(new CborMap(issuerSignedMap, false));
	}
}
