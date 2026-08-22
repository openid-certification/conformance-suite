package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.multipaz.documenttype.knowntypes.DrivingLicense;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateMdocDsCertificateMatchesIssuingCountry_UnitTest {

	private ValidateMdocDsCertificateMatchesIssuingCountry cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new ValidateMdocDsCertificateMatchesIssuingCountry();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	@Test
	public void testEvaluate_passesWhenIssuingCountryMatchesBuiltInCert() throws Exception {
		// built-in DS cert is C=US and VciMdocUtils derives issuing_country from the cert
		MdocCredentialTestUtil.putCredential(env,
			MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_passesForPidDocType() throws Exception {
		MdocCredentialTestUtil.putCredential(env,
			MdocCredentialTestUtil.createCredentialBytes("eu.europa.ec.eudi.pid.1"));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_skipsWhenNoIssuingCountryElement() throws Exception {
		MdocCredentialTestUtil.putCredential(env,
			MdocCredentialTestUtil.createCredentialBytes("net.openid.examples.certification.1.mdoc"));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_flagsCertWithoutCountryName() throws Exception {
		// the fixture DS cert has CN only, while the credential data carries an issuing_country
		MdocCredentialTestUtil.putCredential(env,
			MdocDsCertificateTestFixtures.credentialWithNonMdlEkuDsCert(DrivingLicense.MDL_DOCTYPE));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("no countryName"), e.getMessage());
	}
}
