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
import org.multipaz.cbor.Bstr;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.Tstr;
import org.multipaz.crypto.X509Cert;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateRicalStructure_UnitTest {

	private ValidateRicalStructure cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private RicalTestFixtures.ReaderPki pki;

	@BeforeEach
	public void setUp() {
		cond = new ValidateRicalStructure();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
		pki = RicalTestFixtures.generateReaderPki();
	}

	private void putRical(DataItem ricalMap) {
		RicalTestFixtures.putRical(env, RicalTestFixtures.sign(ricalMap));
	}

	private DataItem goodCertInfo() {
		return RicalTestFixtures.certificateInfo(pki.getCaCert());
	}

	/** A non-trust-anchor entry for a certificate issued by some CA, with the aki F.3.2.2 requires. */
	private static DataItem subCaCertInfo(X509Cert cert) {
		return RicalTestFixtures.certificateInfo(cert, false, null, Set.of(),
			Map.of("aki", new Bstr(cert.getAuthorityKeyIdentifier())));
	}

	@Test
	public void testEvaluate_passesForSpecCompleteRical() {
		putRical(RicalTestFixtures.buildRicalMap(List.of(goodCertInfo())));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenIsTrustAnchorMissing() {
		// the Geneva 2026 interop RICAL omits the required isTrustAnchor field
		putRical(RicalTestFixtures.buildRicalMap(List.of(
			RicalTestFixtures.certificateInfo(pki.getCaCert(), true, null, Set.of("isTrustAnchor"), Map.of()))));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("isTrustAnchor"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenTypeMissing() {
		putRical(RicalTestFixtures.buildRicalMap(List.of(goodCertInfo()),
			"1.0", "OIDF Test RICAL Provider", RicalTestFixtures.now(), null,
			RicalTestFixtures.soon(), 1L, Set.of(), Map.of()));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("type"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsOnUnknownTopLevelField() {
		putRical(RicalTestFixtures.buildRicalMap(List.of(goodCertInfo()),
			"1.0", "OIDF Test RICAL Provider", RicalTestFixtures.now(),
			RicalTestFixtures.READER_AUTHENTICATION_TYPE, RicalTestFixtures.soon(), 1L,
			Set.of(), Map.of("misspelled", new Tstr("value"))));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("misspelled"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenCertificateInfosEmpty() {
		putRical(RicalTestFixtures.buildRicalMap(List.of()));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("empty"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenSkiMismatches() {
		putRical(RicalTestFixtures.buildRicalMap(List.of(
			RicalTestFixtures.certificateInfo(pki.getCaCert(), true,
				new byte[] { 1, 2, 3, 4 }, Set.of(), Map.of()))));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("ski"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenSubCaEntryLacksAki() {
		// the reader (end-entity) certificate is not self-issued, so an entry for it
		// requires the aki field
		putRical(RicalTestFixtures.buildRicalMap(List.of(
			goodCertInfo(),
			RicalTestFixtures.certificateInfo(pki.getReaderCert(), false, null, Set.of(), Map.of()))));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("aki"), e.getMessage());
	}

	@Test
	public void testEvaluate_passesWhenNonAnchorChainsToListedAnchor() {
		putRical(RicalTestFixtures.buildRicalMap(List.of(
			goodCertInfo(),
			subCaCertInfo(pki.getReaderCert()))));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenNonAnchorOnlySharesItsIssuerNameWithAListedAnchor() {
		// an unrelated CA with the same subject name as the listed anchor: a name match must
		// not count as a path
		RicalTestFixtures.ReaderPki sameNamePki = RicalTestFixtures.generateReaderPki();
		putRical(RicalTestFixtures.buildRicalMap(List.of(
			goodCertInfo(),
			subCaCertInfo(sameNamePki.getReaderCert()))));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("its issuer is not listed in the RICAL"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenNonAnchorSignatureDoesNotVerifyWithClaimedIssuer() {
		// names the listed anchor as issuer, by name and AKI, but was not signed by it
		putRical(RicalTestFixtures.buildRicalMap(List.of(
			goodCertInfo(),
			subCaCertInfo(RicalTestFixtures.impostorReaderCert(pki.getCaCert())))));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("its signature does not verify with that entry's key"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenNonAnchorHasNoPathToListedIssuer() {
		RicalTestFixtures.ReaderPki otherPki = RicalTestFixtures.generateReaderPki("Unlisted Reader CA");
		// a non-trust-anchor entry whose issuer is not in the list
		putRical(RicalTestFixtures.buildRicalMap(List.of(
			goodCertInfo(),
			subCaCertInfo(otherPki.getReaderCert()))));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("no path to a trust anchor"), e.getMessage());
	}

	@Test
	public void testEvaluate_flagsNegativeBignumSerialWithHint() {
		putRical(RicalTestFixtures.buildRicalMap(List.of(
			RicalTestFixtures.certificateInfo(pki.getCaCert(), true, null, Set.of(), Map.of(), true))));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("negative-bignum tag by mistake"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsOnStaleNextUpdate() {
		putRical(RicalTestFixtures.buildRicalMap(List.of(goodCertInfo()),
			"1.0", "OIDF Test RICAL Provider", RicalTestFixtures.now(),
			RicalTestFixtures.READER_AUTHENTICATION_TYPE, RicalTestFixtures.past(), 1L,
			Set.of(), Map.of()));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("nextUpdate"), e.getMessage());
	}

	@Test
	public void testEvaluate_acceptsCorrectlySpelledTrustConstraints() {
		putRical(RicalTestFixtures.buildRicalMap(List.of(
			RicalTestFixtures.certificateInfo(pki.getCaCert(), true, null, Set.of(),
				Map.of("trustConstraints", new org.multipaz.cbor.CborArray(new java.util.ArrayList<>(), false))))));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_reportsTheCddlsMisspeltTrustConstraints() {
		// F.3.2.2's CDDL spells the key "trustContraints" while its field list says
		// "trustConstraints"; a list using the typo has its constraints ignored by parsers that
		// follow the field list, so the key must be reported rather than silently accepted
		putRical(RicalTestFixtures.buildRicalMap(List.of(
			RicalTestFixtures.certificateInfo(pki.getCaCert(), true, null, Set.of(),
				Map.of("trustContraints", new org.multipaz.cbor.CborArray(new java.util.ArrayList<>(), false))))));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("trustContraints"), e.getMessage());
	}
}
