package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.lote.LoteSlot;
import net.openid.conformance.condition.client.lote.LoteTestFixtures;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateMdocTrustAnchorIacaCertificateProfile_UnitTest {

	private ValidateMdocTrustAnchorIacaCertificateProfile cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new ValidateMdocTrustAnchorIacaCertificateProfile();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	@Test
	public void testEvaluate_passesForTheSuiteIacaRoot() {
		env.putString("credential_trust_anchor_pem", TestKeysAndCerts.IACA_ROOT_CERT_PEM);

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_skipsWhenNoTrustAnchorConfigured() {
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_checksTheListedIacaThatIssuedTheDocumentSigner() throws Exception {
		LoteTestFixtures.ListedCa ca = LoteTestFixtures.INSTANCE.generateCa("Listed CA", "Example Provider");
		env.putString("mdoc_credential_cbor", Base64.getEncoder().encodeToString(MdocDsCertificateTestFixtures.credentialSignedByLeafUnder(ca)));
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.PID, LoteTestFixtures.PID_ISSUANCE, "Example Provider", ca.getCert());

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("listed in the configured list of trusted entities"), e.getMessage());
		assertTrue(e.getMessage().contains("issuer alternative name"), e.getMessage());
	}

	@Test
	public void testEvaluate_skipsWhenNoListedAnchorIssuedTheDocumentSigner() throws Exception {
		LoteTestFixtures.ListedCa ca = LoteTestFixtures.INSTANCE.generateCa("Listed CA", "Example Provider");
		LoteTestFixtures.ListedCa other = LoteTestFixtures.INSTANCE.generateCa("Other CA", "Other Provider");
		env.putString("mdoc_credential_cbor", Base64.getEncoder().encodeToString(MdocDsCertificateTestFixtures.credentialSignedByLeafUnder(ca)));
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.PID, LoteTestFixtures.PID_ISSUANCE, "Other Provider", other.getCert());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_flagsNonIacaCertificate() throws Exception {
		// self-signed CN-only cert: no countryName, no keyUsage, no issuerAltName, pathLen absent
		env.putString("credential_trust_anchor_pem",
			MdocDsCertificateTestFixtures.selfSignedCertPem("CN=Plain Test CA"));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("countryName"), e.getMessage());
		assertTrue(e.getMessage().contains("keyUsage"), e.getMessage());
		assertTrue(e.getMessage().contains("issuer alternative name"), e.getMessage());
	}
}
