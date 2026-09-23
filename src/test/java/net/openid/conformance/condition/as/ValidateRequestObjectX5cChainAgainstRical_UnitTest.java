package net.openid.conformance.condition.as;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.RicalTestFixtures;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateRequestObjectX5cChainAgainstRical_UnitTest {

	private ValidateRequestObjectX5cChainAgainstRical cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private RicalTestFixtures.ReaderPki pki;

	@BeforeEach
	public void setUp() {
		cond = new ValidateRequestObjectX5cChainAgainstRical();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
		pki = RicalTestFixtures.generateReaderPki();
	}

	@Test
	public void testEvaluate_passesWhenReaderCaListed() {
		RicalTestFixtures.putSignedRequestObject(env, pki);
		RicalTestFixtures.putRical(env, RicalTestFixtures.goodSignedRical(List.of(pki.getCaCert())));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenReaderCaNotListed() {
		RicalTestFixtures.putSignedRequestObject(env, pki);
		RicalTestFixtures.ReaderPki otherPki = RicalTestFixtures.generateReaderPki();
		RicalTestFixtures.putRical(env, RicalTestFixtures.goodSignedRical(List.of(otherPki.getCaCert())));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not chain"), e.getMessage());
	}

	@Test
	public void testEvaluate_passesDespiteMisTaggedSerialNumberEntry() {
		// the Geneva 2026 interop RICAL has one entry whose serialNumber uses the
		// negative-bignum tag; multipaz takes the serial from the embedded certificate and
		// parses the list regardless, so the rest of the list stays usable for the trust
		// evaluation. The defect itself is reported by ValidateRicalStructure.
		RicalTestFixtures.putSignedRequestObject(env, pki);
		RicalTestFixtures.ReaderPki brokenEntryPki = RicalTestFixtures.generateReaderPki("Mis-encoded Reader CA");
		RicalTestFixtures.putRical(env, RicalTestFixtures.sign(RicalTestFixtures.buildRicalMap(List.of(
			RicalTestFixtures.certificateInfo(pki.getCaCert()),
			RicalTestFixtures.certificateInfo(brokenEntryPki.getCaCert(), true, null,
				java.util.Set.of(), java.util.Map.of(), true)))));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenRequestObjectHasNoX5c() {
		// the x509_san_dns / x509_hash prefixes reference the certificate in the x5c header, so
		// a request object without one leaves nothing to evaluate against the RICAL
		RicalTestFixtures.putSignedRequestObject(env, pki, false);
		RicalTestFixtures.putRical(env, RicalTestFixtures.goodSignedRical(List.of(pki.getCaCert())));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("x5c"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenListedEntryIsNotATrustAnchor() {
		// F.3.2.6: the trust anchor is the CertificateInfo with isTrustAnchor true that is
		// highest in the path - a chain reaching only isTrustAnchor=false entries is not trusted,
		// though multipaz's RicalTrustManager makes a trust point of every listed entry
		RicalTestFixtures.putSignedRequestObject(env, pki);
		RicalTestFixtures.putRical(env, RicalTestFixtures.sign(RicalTestFixtures.buildRicalMap(
			List.of(RicalTestFixtures.certificateInfo(pki.getCaCert(), false)))));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("isTrustAnchor"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenRicalNotAfterHasPassed() {
		// F.3.2.5: "if present, notAfter shall not be in the past" - an expired list's entries
		// cannot be used as trust anchors
		RicalTestFixtures.putSignedRequestObject(env, pki);
		RicalTestFixtures.putRical(env, RicalTestFixtures.sign(RicalTestFixtures.buildRicalMap(
			List.of(RicalTestFixtures.certificateInfo(pki.getCaCert())),
			"1.0", "OIDF Test RICAL Provider", RicalTestFixtures.past(),
			RicalTestFixtures.READER_AUTHENTICATION_TYPE, RicalTestFixtures.soon(), 1L,
			java.util.Set.of(), java.util.Map.of(), RicalTestFixtures.past())));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("notAfter"), e.getMessage());
	}

	@Test
	public void testEvaluate_passesWhenReaderCertificateItselfListed() {
		// the RICAL may list the end-entity certificate rather than its CA
		RicalTestFixtures.putSignedRequestObject(env, pki);
		RicalTestFixtures.putRical(env, RicalTestFixtures.sign(RicalTestFixtures.buildRicalMap(
			List.of(RicalTestFixtures.certificateInfo(pki.getReaderCert())))));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenRicalSignatureBroken() {
		RicalTestFixtures.putSignedRequestObject(env, pki);
		byte[] rical = RicalTestFixtures.goodSignedRical(List.of(pki.getCaCert()));
		rical[rical.length / 2] ^= 0x01;
		RicalTestFixtures.putRical(env, rical);

		// the specific failure from the signature check itself, not the caller's generic wrapper:
		// the helpers log their own finding before throwing, so that is the one reported
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("COSE_Sign1 signature verification failed"), e.getMessage());
	}
}
