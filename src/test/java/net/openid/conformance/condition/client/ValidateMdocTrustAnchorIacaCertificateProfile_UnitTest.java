package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

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
