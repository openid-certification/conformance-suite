package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.ValidateMdocIssuerSignedSignature;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.MdocUtil;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.cose.Cose;
import org.multipaz.cose.CoseNumberLabel;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.X509CertChain;
import org.multipaz.crypto.X509CertJvmKt;
import org.multipaz.mdoc.mso.MobileSecurityObject;
import org.multipaz.revocation.RevocationStatus;

import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class CreateMdocCredentialForVCI_UnitTest {

	private static final String DEVICE_PUBLIC_KEY_JWK = """
		{
			"kty": "EC",
			"crv": "P-256",
			"x": "cwYyuS94hcOtcPlrMMtGtflCfbZUwz5Mf1Gfa2m0AM8",
			"y": "KB7sJkFQyB8jZHO9vmWS5LNECL4id3OJO9HX9ChNonA"
		}
		""";

	private static final String ISSUER_SIGNING_KEY_JWK = """
		{
		    "kty": "EC",
		    "d": "y2NSNIvlRAEBMFk2bjQcSKbjS1y_NBJQ6jRzIfuIxS0",
		    "use": "sig",
		    "x5c": [
		        "MIIB+DCCAZ6gAwIBAgIUSy80Ezru1eOPrGW88uSFC8H8lVYwCgYIKoZIzj0EAwIwITELMAkGA1UEBhMCR0IxEjAQBgNVBAMMCU9JREYgVGVzdDAeFw0yNDExMTkwOTMwMzNaFw0zNDExMTcwOTMwMzNaMCExCzAJBgNVBAYTAkdCMRIwEAYDVQQDDAlPSURGIFRlc3QwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATT/dLsd51LLBrGV6R23o6vymRxHXeFBoI8yq31y5kFV2VV0gi9x5ZzEFiq8DMiAHucLACFndxLtZorCha9zznQo4GzMIGwMB0GA1UdDgQWBBS5cbdgAeMBi5wxpbpwISGhShAWETAfBgNVHSMEGDAWgBS5cbdgAeMBi5wxpbpwISGhShAWETAPBgNVHRMBAf8EBTADAQH/MF0GA1UdEQRWMFSCEHd3dy5oZWVuYW4ubWUudWuCHWRlbW8uY2VydGlmaWNhdGlvbi5vcGVuaWQubmV0gglsb2NhbGhvc3SCFmxvY2FsaG9zdC5lbW9iaXguY28udWswCgYIKoZIzj0EAwIDSAAwRQIhAPQtPciRiOPkw4ZMfmP1ov3LXlhG8wizrJ9Oyu+QPWAEAiBJn30EEuhhFyS7nqOhZok+M0XNbbxhNB0i7KxKSEsITA=="
		    ],
		    "crv": "P-256",
		    "kid": "5H1WLeSx55tMW6JNlvqMfg3O_E0eQPqB8jDSoUn6oiI",
		    "x": "0_3S7HedSywaxlekdt6Or8pkcR13hQaCPMqt9cuZBVc",
		    "y": "ZVXSCL3HlnMQWKrwMyIAe5wsAIWd3Eu1misKFr3POdA",
		    "alg": "ES256"
		}
		""";

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CreateMdocCredentialForVCI cond;

	@BeforeEach
	public void setUp() {
		cond = new CreateMdocCredentialForVCI();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * The configured signing JWK is for SD-JWT VCs only: mdocs are always signed with the suite's
	 * own document signer certificate, issued under its mdoc IACA root, so that wallets which trust
	 * that root can validate them.
	 */
	@Test
	public void testEvaluate_ignoresConfiguredSigningKeyAndSignsUnderTheIacaRoot() throws Exception {
		putCredentialRequest();

		cond.execute(env);

		JsonObject issuance = env.getObject("credential_issuance");
		assertThat(issuance).isNotNull();
		JsonArray credentials = issuance.getAsJsonArray("credentials");
		assertThat(credentials).isNotNull();
		assertThat(credentials.size()).isEqualTo(1);

		String credential = OIDFJSON.getString(credentials.get(0).getAsJsonObject().get("credential"));
		assertThat(credential).isNotBlank();

		byte[] credentialBytes = new Base64URL(credential).decode();
		DataItem issuerSigned = Cbor.INSTANCE.decode(credentialBytes);
		DataItem issuerAuth = issuerSigned.getOrNull("issuerAuth");
		assertThat(issuerAuth).isNotNull();
		CoseSign1 issuerAuthSign1 = issuerAuth.getAsCoseSign1();
		DataItem x5chainDataItem = issuerAuthSign1.getUnprotectedHeaders()
			.get(new CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN));
		assertThat(x5chainDataItem).isNotNull();
		X509CertChain certChain = x5chainDataItem.getAsX509CertChain();
		assertThat(certChain.getCertificates()).hasSize(1);
		X509Certificate dsCert = toJava(certChain.getCertificates().get(0));
		X509Certificate iacaCert = toJava(TestKeysAndCerts.getIacaRootCert());
		// not the certificate from the configured signing JWK, but the suite's own document signer
		assertThat(dsCert.getSubjectX500Principal().getName()).doesNotContain("CN=OIDF Test");
		assertThat(dsCert.getIssuerX500Principal()).isEqualTo(iacaCert.getSubjectX500Principal());
		dsCert.verify(iacaCert.getPublicKey());

		Environment verifyEnv = new Environment();
		verifyEnv.putString("mdoc_credential_cbor", Base64.getEncoder().encodeToString(credentialBytes));
		ValidateMdocIssuerSignedSignature verifyCond = new ValidateMdocIssuerSignedSignature();
		verifyCond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		verifyCond.execute(verifyEnv);
	}

	@Test
	public void testEvaluate_noStatusListReferenceByDefault() {
		putCredentialRequest();

		cond.execute(env);

		assertThat(parseMso().getRevocationStatus()).isNull();
	}

	@Test
	public void testEvaluate_embedsStatusListReferenceWhenConfigured() {
		cond = new CreateMdocCredentialForVCI("https://issuer.example.com/statuslists/1");
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		putCredentialRequest();

		cond.execute(env);

		RevocationStatus revocationStatus = parseMso().getRevocationStatus();
		assertThat(revocationStatus).isInstanceOf(RevocationStatus.StatusList.class);
		RevocationStatus.StatusList statusList = (RevocationStatus.StatusList) revocationStatus;
		assertThat(statusList.getUri()).isEqualTo("https://issuer.example.com/statuslists/1");
		// the served list marks even indices valid, so only even indices may be issued
		assertThat(statusList.getIdx() % 2).isEqualTo(0);
		assertThat(statusList.getIdx()).isBetween(0, 254);
	}

	private void putCredentialRequest() {
		env.putString("proof_type", "jwt");
		env.putObjectFromJsonString("proof_jwt", "header.jwk", DEVICE_PUBLIC_KEY_JWK);
		env.putObjectFromJsonString("credential_configuration", """
			{
				"doctype": "org.iso.18013.5.1.mDL",
				"cryptographic_binding_methods_supported": ["jwk"]
			}
			""");
		env.putObjectFromJsonString("config", "credential.signing_jwk", ISSUER_SIGNING_KEY_JWK);
	}

	private static X509Certificate toJava(org.multipaz.crypto.X509Cert cert) {
		return X509CertJvmKt.getJavaX509Certificate(cert);
	}

	private MobileSecurityObject parseMso() {
		JsonArray credentials = env.getObject("credential_issuance").getAsJsonArray("credentials");
		String credential = OIDFJSON.getString(credentials.get(0).getAsJsonObject().get("credential"));
		try {
			return MdocUtil.parseMso(new Base64URL(credential).decode());
		} catch (MdocUtil.MdocParseException e) {
			throw new AssertionError(e);
		}
	}
}
