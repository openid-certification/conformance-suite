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
import org.multipaz.testapp.VciMdocUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateMdocIssuerSignedSignature_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateMdocIssuerSignedSignature cond;

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
		cond = new ValidateMdocIssuerSignedSignature();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_validSignature() {
		// Generate a valid mdoc credential using VciMdocUtils
		String mdocBase64Url = VciMdocUtils.createMdocCredential(
			DEVICE_KEY_JWK, "org.iso.18013.5.1.mDL", null);

		// Decode from base64url to raw bytes, then re-encode as standard base64
		byte[] bytes = new Base64URL(mdocBase64Url).decode();
		env.putString("mdoc_credential_cbor", Base64.getEncoder().encodeToString(bytes));

		// Should succeed without throwing
		cond.execute(env);
	}

	@Test
	public void testEvaluate_tamperedSignature() {
		// Generate a valid mdoc credential
		String mdocBase64Url = VciMdocUtils.createMdocCredential(
			DEVICE_KEY_JWK, "org.iso.18013.5.1.mDL", null);

		// Decode, tamper with the bytes, re-encode
		byte[] bytes = new Base64URL(mdocBase64Url).decode();

		// Tamper with the last few bytes (part of the COSE_Sign1 signature)
		if (bytes.length > 10) {
			bytes[bytes.length - 1] = (byte) (bytes[bytes.length - 1] ^ 0xFF);
			bytes[bytes.length - 2] = (byte) (bytes[bytes.length - 2] ^ 0xFF);
			bytes[bytes.length - 3] = (byte) (bytes[bytes.length - 3] ^ 0xFF);
		}

		env.putString("mdoc_credential_cbor", Base64.getEncoder().encodeToString(bytes));

		// Should throw ConditionError due to signature verification failure
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
