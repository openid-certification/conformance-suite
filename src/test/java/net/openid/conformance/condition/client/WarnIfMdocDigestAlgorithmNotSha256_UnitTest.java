package net.openid.conformance.condition.client;

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
import org.multipaz.cbor.Bstr;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.Tagged;
import org.multipaz.cbor.Tstr;
import org.multipaz.cose.CoseSign1;
import org.multipaz.testapp.VciMdocUtils;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class WarnIfMdocDigestAlgorithmNotSha256_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnIfMdocDigestAlgorithmNotSha256 cond;

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
		cond = new WarnIfMdocDigestAlgorithmNotSha256();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private static byte[] suiteIssuedMdoc() {
		String mdocBase64Url = VciMdocUtils.createMdocCredential(
			DEVICE_KEY_JWK, "org.iso.18013.5.1.mDL", null);
		return new Base64URL(mdocBase64Url).decode();
	}

	@Test
	public void testEvaluate_sha256() {
		// VciMdocUtils creates mdocs whose MSO uses SHA-256
		env.putString("mdoc_credential_cbor", Base64.getEncoder().encodeToString(suiteIssuedMdoc()));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_sha384Throws() {
		env.putString("mdoc_credential_cbor", Base64.getEncoder().encodeToString(mdocWithDigestAlgorithm("SHA-384")));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_sha512Throws() {
		env.putString("mdoc_credential_cbor", Base64.getEncoder().encodeToString(mdocWithDigestAlgorithm("SHA-512")));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_notAnMdocThrows() {
		env.putString("mdoc_credential_cbor", Base64.getEncoder().encodeToString(new byte[] { 0x01 }));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	/** A suite-issued mdoc with the MSO's digestAlgorithm replaced (IssuerSigned → issuerAuth → MSO). */
	private static byte[] mdocWithDigestAlgorithm(String digestAlgorithm) {
		DataItem issuerSignedItem = Cbor.INSTANCE.decode(suiteIssuedMdoc());
		CoseSign1 originalCose = issuerSignedItem.get("issuerAuth").getAsCoseSign1();

		DataItem msoDataItem = Cbor.INSTANCE.decode(originalCose.getPayload()).getAsTaggedEncodedCbor();
		Map<DataItem, DataItem> msoMap = new LinkedHashMap<>(msoDataItem.getAsMap());
		msoMap.put(new Tstr("digestAlgorithm"), new Tstr(digestAlgorithm));

		byte[] newMsoBytes = Cbor.INSTANCE.encode(new CborMap(msoMap, false));
		byte[] newPayload = Cbor.INSTANCE.encode(
			new Tagged(Tagged.ENCODED_CBOR, new Bstr(newMsoBytes)));

		CoseSign1 newCose = new CoseSign1(
			originalCose.getProtectedHeaders(),
			originalCose.getUnprotectedHeaders(),
			originalCose.getSignature(),
			newPayload);

		Map<DataItem, DataItem> issuerSignedMap = new LinkedHashMap<>(issuerSignedItem.getAsMap());
		issuerSignedMap.put(new Tstr("issuerAuth"),
			Cbor.INSTANCE.decode(Cbor.INSTANCE.encode(newCose.toDataItem())));
		return Cbor.INSTANCE.encode(new CborMap(issuerSignedMap, false));
	}
}
