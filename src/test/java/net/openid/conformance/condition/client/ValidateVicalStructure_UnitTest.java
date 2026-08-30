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
import org.multipaz.cbor.Tagged;
import org.multipaz.cbor.Tstr;

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
public class ValidateVicalStructure_UnitTest {

	private ValidateVicalStructure cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private VicalTestFixtures.VicalSigner iaca;

	@BeforeEach
	public void setUp() {
		cond = new ValidateVicalStructure();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
		iaca = VicalTestFixtures.generateSigner();
	}

	private void putVical(DataItem vicalMap) {
		VicalTestFixtures.putVical(env, VicalTestFixtures.sign(vicalMap));
	}

	private DataItem goodCertInfo() {
		return VicalTestFixtures.certificateInfo(iaca.getCert());
	}

	@Test
	public void testEvaluate_passesForSpecCompleteVical() {
		putVical(VicalTestFixtures.buildVicalMap(List.of(goodCertInfo())));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_acceptsSecondEditionNotAfterAndVicalUrl() {
		putVical(VicalTestFixtures.buildVicalMap(List.of(goodCertInfo()),
			"1.0", "OIDF Test VICAL Provider", VicalTestFixtures.now(), VicalTestFixtures.soon(), 1L,
			Set.of(), Map.of(
				"notAfter", new Tagged(Tagged.DATE_TIME_STRING, new Tstr(VicalTestFixtures.soon().toString())),
				"vicalURL", new Tstr("https://vical.example.com/vical.cbor"))));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenDateImplausiblyOld() {
		putVical(VicalTestFixtures.buildVicalMap(List.of(goodCertInfo()),
			"1.0", "OIDF Test VICAL Provider",
			kotlin.time.Instant.Companion.parse("2010-06-01T00:00:00Z"), null, 1L,
			Set.of(), Map.of()));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("date");
	}

	@Test
	public void testEvaluate_failsWhenNextUpdateImplausiblyFarInFuture() {
		putVical(VicalTestFixtures.buildVicalMap(List.of(goodCertInfo()),
			"1.0", "OIDF Test VICAL Provider", VicalTestFixtures.now(),
			kotlin.time.Instant.Companion.parse("2150-01-01T00:00:00Z"), 1L,
			Set.of(), Map.of()));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("nextUpdate");
	}

	@Test
	public void testEvaluate_failsWhenNotAfterInPast() {
		putVical(VicalTestFixtures.buildVicalMap(List.of(goodCertInfo()),
			"1.0", "OIDF Test VICAL Provider", VicalTestFixtures.now(), VicalTestFixtures.soon(), 1L,
			Set.of(), Map.of(
				"notAfter", new Tagged(Tagged.DATE_TIME_STRING, new Tstr(VicalTestFixtures.past().toString())))));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("notAfter");
	}

	@Test
	public void testEvaluate_failsWhenVersionMissing() {
		putVical(VicalTestFixtures.buildVicalMap(List.of(goodCertInfo()),
			"1.0", "OIDF Test VICAL Provider", VicalTestFixtures.now(), VicalTestFixtures.soon(), 1L,
			Set.of("version"), Map.of()));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("version");
	}

	@Test
	public void testEvaluate_failsWhenDateInFuture() {
		putVical(VicalTestFixtures.buildVicalMap(List.of(goodCertInfo()),
			"1.0", "OIDF Test VICAL Provider", VicalTestFixtures.soon(), VicalTestFixtures.soon(), 1L,
			Set.of(), Map.of()));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("date");
	}

	@Test
	public void testEvaluate_failsWhenNextUpdateInPast() {
		putVical(VicalTestFixtures.buildVicalMap(List.of(goodCertInfo()),
			"1.0", "OIDF Test VICAL Provider", VicalTestFixtures.now(), VicalTestFixtures.past(), 1L,
			Set.of(), Map.of()));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("nextUpdate");
	}

	@Test
	public void testEvaluate_failsOnUnknownTopLevelKey() {
		putVical(VicalTestFixtures.buildVicalMap(List.of(goodCertInfo()),
			"1.0", "OIDF Test VICAL Provider", VicalTestFixtures.now(), VicalTestFixtures.soon(), 1L,
			Set.of(), Map.of("customField", new Tstr("x"))));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("customField");
	}

	@Test
	public void testEvaluate_failsOnSkiMismatch() {
		DataItem badSki = VicalTestFixtures.certificateInfo(iaca.getCert(),
			List.of("org.iso.18013.5.1.mDL"), new byte[] { 1, 2, 3, 4 }, null, Set.of(), Map.of());
		putVical(VicalTestFixtures.buildVicalMap(List.of(badSki)));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("ski");
	}

	@Test
	public void testEvaluate_failsOnSerialNumberMismatch() {
		DataItem badSerial = VicalTestFixtures.certificateInfo(iaca.getCert(),
			List.of("org.iso.18013.5.1.mDL"), null, new byte[] { 0x7f, 0x7f }, Set.of(), Map.of());
		putVical(VicalTestFixtures.buildVicalMap(List.of(badSerial)));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("serialNumber");
	}

	@Test
	public void testEvaluate_negativeBignumSerialFindingNamesTagAndCertificate() {
		// the real-world defect from the Geneva interop VICAL: a serial number with the high
		// bit set encoded as a tag 3 negative bignum instead of a tag 2 unsigned bignum
		byte[] serialBytes = iaca.getCert().getSerialNumber().getValue();
		DataItem wrongTagSerial = VicalTestFixtures.certificateInfo(iaca.getCert(),
			List.of("org.iso.18013.5.1.mDL"), null, null, Set.of("serialNumber"),
			Map.of("serialNumber", new Tagged(Tagged.NEGATIVE_BIGNUM, new Bstr(serialBytes))));
		putVical(VicalTestFixtures.buildVicalMap(List.of(wrongTagSerial)));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("tag 3 (negative bignum)");
		assertFindingLogged("negative-bignum tag by mistake");
		// the finding names the certificate, not just the array index
		assertFindingLogged("CN=OIDF Test VICAL Signer");
	}

	@Test
	public void testEvaluate_failsOnEmptyDocTypes() {
		DataItem noDocTypes = VicalTestFixtures.certificateInfo(iaca.getCert(),
			List.of(), null, null, Set.of(), Map.of());
		putVical(VicalTestFixtures.buildVicalMap(List.of(noDocTypes)));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("docType");
	}

	@Test
	public void testEvaluate_invalidEntryDateFindingNamesTheEntry() {
		DataItem badNotBefore = VicalTestFixtures.certificateInfo(iaca.getCert(),
			List.of("org.iso.18013.5.1.mDL"), null, null, Set.of(),
			Map.of("notBefore", new Tstr("2020-01-01T00:00:00Z")));
		putVical(VicalTestFixtures.buildVicalMap(List.of(badNotBefore)));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("certificateInfos[0]");
		assertFindingLogged("notBefore");
	}

	@Test
	public void testEvaluate_failsWhenCertificateInfoMissingSki() {
		DataItem noSki = VicalTestFixtures.certificateInfo(iaca.getCert(),
			List.of("org.iso.18013.5.1.mDL"), null, null, Set.of("ski"), Map.of());
		putVical(VicalTestFixtures.buildVicalMap(List.of(noSki)));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFindingLogged("ski");
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
