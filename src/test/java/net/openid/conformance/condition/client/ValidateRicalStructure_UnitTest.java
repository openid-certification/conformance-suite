package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

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

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("isTrustAnchor");
	}

	@Test
	public void testEvaluate_failsWhenTypeMissing() {
		putRical(RicalTestFixtures.buildRicalMap(List.of(goodCertInfo()),
			"1.0", "OIDF Test RICAL Provider", RicalTestFixtures.now(), null,
			RicalTestFixtures.soon(), 1L, Set.of(), Map.of()));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("type");
	}

	@Test
	public void testEvaluate_failsOnUnknownTopLevelField() {
		putRical(RicalTestFixtures.buildRicalMap(List.of(goodCertInfo()),
			"1.0", "OIDF Test RICAL Provider", RicalTestFixtures.now(),
			RicalTestFixtures.READER_AUTHENTICATION_TYPE, RicalTestFixtures.soon(), 1L,
			Set.of(), Map.of("misspelled", new Tstr("value"))));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("misspelled");
	}

	@Test
	public void testEvaluate_failsWhenCertificateInfosEmpty() {
		putRical(RicalTestFixtures.buildRicalMap(List.of()));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("empty");
	}

	@Test
	public void testEvaluate_failsWhenSkiMismatches() {
		putRical(RicalTestFixtures.buildRicalMap(List.of(
			RicalTestFixtures.certificateInfo(pki.getCaCert(), true,
				new byte[] { 1, 2, 3, 4 }, Set.of(), Map.of()))));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("ski");
	}

	@Test
	public void testEvaluate_failsWhenSubCaEntryLacksAki() {
		// the reader (end-entity) certificate is not self-issued, so an entry for it
		// requires the aki field
		putRical(RicalTestFixtures.buildRicalMap(List.of(
			goodCertInfo(),
			RicalTestFixtures.certificateInfo(pki.getReaderCert(), false, null, Set.of(), Map.of()))));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("aki");
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

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("its issuer is not listed in the RICAL");
	}

	@Test
	public void testEvaluate_failsWhenNonAnchorSignatureDoesNotVerifyWithClaimedIssuer() {
		// names the listed anchor as issuer, by name and AKI, but was not signed by it
		putRical(RicalTestFixtures.buildRicalMap(List.of(
			goodCertInfo(),
			subCaCertInfo(RicalTestFixtures.impostorReaderCert(pki.getCaCert())))));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("its signature does not verify with that entry's key");
	}

	@Test
	public void testEvaluate_failsWhenNonAnchorHasNoPathToListedIssuer() {
		RicalTestFixtures.ReaderPki otherPki = RicalTestFixtures.generateReaderPki("Unlisted Reader CA");
		// a non-trust-anchor entry whose issuer is not in the list
		putRical(RicalTestFixtures.buildRicalMap(List.of(
			goodCertInfo(),
			subCaCertInfo(otherPki.getReaderCert()))));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("no path to a trust anchor");
	}

	@Test
	public void testEvaluate_flagsNegativeBignumSerialWithHint() {
		putRical(RicalTestFixtures.buildRicalMap(List.of(
			RicalTestFixtures.certificateInfo(pki.getCaCert(), true, null, Set.of(), Map.of(), true))));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("negative-bignum tag by mistake");
	}

	@Test
	public void testEvaluate_failsOnStaleNextUpdate() {
		putRical(RicalTestFixtures.buildRicalMap(List.of(goodCertInfo()),
			"1.0", "OIDF Test RICAL Provider", RicalTestFixtures.now(),
			RicalTestFixtures.READER_AUTHENTICATION_TYPE, RicalTestFixtures.past(), 1L,
			Set.of(), Map.of()));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("nextUpdate");
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

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("trustContraints");
	}

	/**
	 * The per-entry findings are logged in the failure entry's 'findings' detail rather than
	 * carried in the ConditionError message, so assertions capture the logged map.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private void assertFindingLogged(String text) {
		ArgumentCaptor<Map> logged = ArgumentCaptor.forClass(Map.class);
		verify(eventLog, atLeastOnce()).log(anyString(), logged.capture());
		StringBuilder findings = new StringBuilder();
		for (Map entry : logged.getAllValues()) {
			findings.append(entry.get("findings")).append('\n');
		}
		assertTrue(findings.toString().contains(text), findings.toString());
	}
}
