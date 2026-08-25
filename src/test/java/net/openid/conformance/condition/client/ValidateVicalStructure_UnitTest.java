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
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.Tagged;
import org.multipaz.cbor.Tstr;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("date"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenNextUpdateImplausiblyFarInFuture() {
		putVical(VicalTestFixtures.buildVicalMap(List.of(goodCertInfo()),
			"1.0", "OIDF Test VICAL Provider", VicalTestFixtures.now(),
			kotlin.time.Instant.Companion.parse("2150-01-01T00:00:00Z"), 1L,
			Set.of(), Map.of()));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("nextUpdate"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenNotAfterInPast() {
		putVical(VicalTestFixtures.buildVicalMap(List.of(goodCertInfo()),
			"1.0", "OIDF Test VICAL Provider", VicalTestFixtures.now(), VicalTestFixtures.soon(), 1L,
			Set.of(), Map.of(
				"notAfter", new Tagged(Tagged.DATE_TIME_STRING, new Tstr(VicalTestFixtures.past().toString())))));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("notAfter"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenVersionMissing() {
		putVical(VicalTestFixtures.buildVicalMap(List.of(goodCertInfo()),
			"1.0", "OIDF Test VICAL Provider", VicalTestFixtures.now(), VicalTestFixtures.soon(), 1L,
			Set.of("version"), Map.of()));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("version"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenDateInFuture() {
		putVical(VicalTestFixtures.buildVicalMap(List.of(goodCertInfo()),
			"1.0", "OIDF Test VICAL Provider", VicalTestFixtures.soon(), VicalTestFixtures.soon(), 1L,
			Set.of(), Map.of()));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("date"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenNextUpdateInPast() {
		putVical(VicalTestFixtures.buildVicalMap(List.of(goodCertInfo()),
			"1.0", "OIDF Test VICAL Provider", VicalTestFixtures.now(), VicalTestFixtures.past(), 1L,
			Set.of(), Map.of()));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("nextUpdate"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsOnUnknownTopLevelKey() {
		putVical(VicalTestFixtures.buildVicalMap(List.of(goodCertInfo()),
			"1.0", "OIDF Test VICAL Provider", VicalTestFixtures.now(), VicalTestFixtures.soon(), 1L,
			Set.of(), Map.of("customField", new Tstr("x"))));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("customField"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsOnSkiMismatch() {
		DataItem badSki = VicalTestFixtures.certificateInfo(iaca.getCert(),
			List.of("org.iso.18013.5.1.mDL"), new byte[] { 1, 2, 3, 4 }, null, Set.of(), Map.of());
		putVical(VicalTestFixtures.buildVicalMap(List.of(badSki)));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("ski"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsOnSerialNumberMismatch() {
		DataItem badSerial = VicalTestFixtures.certificateInfo(iaca.getCert(),
			List.of("org.iso.18013.5.1.mDL"), null, new byte[] { 0x7f, 0x7f }, Set.of(), Map.of());
		putVical(VicalTestFixtures.buildVicalMap(List.of(badSerial)));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("serialNumber"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsOnEmptyDocTypes() {
		DataItem noDocTypes = VicalTestFixtures.certificateInfo(iaca.getCert(),
			List.of(), null, null, Set.of(), Map.of());
		putVical(VicalTestFixtures.buildVicalMap(List.of(noDocTypes)));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("docType"), e.getMessage());
	}

	@Test
	public void testEvaluate_invalidEntryDateFindingNamesTheEntry() {
		DataItem badNotBefore = VicalTestFixtures.certificateInfo(iaca.getCert(),
			List.of("org.iso.18013.5.1.mDL"), null, null, Set.of(),
			Map.of("notBefore", new Tstr("2020-01-01T00:00:00Z")));
		putVical(VicalTestFixtures.buildVicalMap(List.of(badNotBefore)));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("certificateInfos[0]"), e.getMessage());
		assertTrue(e.getMessage().contains("notBefore"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenCertificateInfoMissingSki() {
		DataItem noSki = VicalTestFixtures.certificateInfo(iaca.getCert(),
			List.of("org.iso.18013.5.1.mDL"), null, null, Set.of("ski"), Map.of());
		putVical(VicalTestFixtures.buildVicalMap(List.of(noSki)));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("ski"), e.getMessage());
	}
}
