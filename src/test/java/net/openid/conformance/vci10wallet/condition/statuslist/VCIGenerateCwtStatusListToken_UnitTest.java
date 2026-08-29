package net.openid.conformance.vci10wallet.condition.statuslist;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.Tagged;
import org.multipaz.cose.Cose;
import org.multipaz.cose.CoseNumberLabel;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.X509CertChain;
import org.multipaz.crypto.X509CertJvmKt;

import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class VCIGenerateCwtStatusListToken_UnitTest {

	private static final String ISSUER = "https://issuer.example.com/";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIGenerateCwtStatusListToken cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIGenerateCwtStatusListToken();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putString("server", "issuer", ISSUER);
		env.putString("current_status_list_id", "1");
	}

	/**
	 * ISO/IEC 18013-5 12.3.6.2: with no Certificate element in the MSO's status element, the
	 * revocation list signer's certificate must be signed by the CA that signed the MSO signer's
	 * certificate — the suite's mdoc IACA root.
	 */
	@Test
	public void testEvaluate_signerCertificateChainsToTheIacaRoot() throws Exception {
		cond.execute(env);

		byte[] token = Base64.getDecoder().decode(env.getString("current_status_list_cwt"));
		Tagged tagged = (Tagged) Cbor.INSTANCE.decode(token);
		CoseSign1 coseSign1 = CoseSign1.Companion.fromDataItem(tagged.getTaggedItem());
		DataItem x5chain = coseSign1.getProtectedHeaders()
			.get(new CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN));
		assertNotNull(x5chain);
		X509CertChain certChain = x5chain.getAsX509CertChain();
		assertThat(certChain.getCertificates()).hasSize(1);

		X509Certificate signerCert = toJava(certChain.getCertificates().get(0));
		X509Certificate iacaCert = toJava(TestKeysAndCerts.getIacaRootCert());
		assertThat(signerCert.getIssuerX500Principal()).isEqualTo(iacaCert.getSubjectX500Principal());
		signerCert.verify(iacaCert.getPublicKey());
	}

	private static X509Certificate toJava(org.multipaz.crypto.X509Cert cert) {
		return X509CertJvmKt.getJavaX509Certificate(cert);
	}
}
