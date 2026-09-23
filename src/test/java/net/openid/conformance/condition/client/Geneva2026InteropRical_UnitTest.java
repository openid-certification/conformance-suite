package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.as.EnsureMatchedRicalEntryHasNoTrustConstraints;
import net.openid.conformance.condition.as.ValidateRequestObjectX5cChainAgainstRical;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.BrainpoolSignatureProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Runs the RICAL conditions against a real-world list, the Geneva 2026 interop RICAL (see
 * {@link RicalTestFixtures#geneva2026InteropRical()}), so the generated fixtures' assumptions
 * about what providers actually publish are checked against one. The conditions' clock is
 * pinned to the day the list was fetched, so the list's and the chain's validity windows are
 * judged the same way whenever the test runs.
 */
@ExtendWith(MockitoExtension.class)
public class Geneva2026InteropRical_UnitTest {

	private static final Instant FETCHED_AT = Instant.parse("2026-09-22T12:00:00Z");

	private static final int ENTRY_COUNT = 36;

	// entries whose certificate is not self-issued but which carry no aki
	private static final Set<Integer> SUB_CA_ENTRIES_WITHOUT_AKI = Set.of(1, 18, 23);

	// the entry whose serialNumber is encoded with the negative-bignum tag
	private static final int MIS_TAGGED_SERIAL_ENTRY = 27;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		// the list is signed with a brainpoolP256r1 key; the application installs this
		// provider at startup, the unit test JVM does not
		BrainpoolSignatureProvider.ensureInstalled();
		env = new Environment();
		RicalTestFixtures.putRical(env, RicalTestFixtures.geneva2026InteropRical());
	}

	private <T extends AbstractCondition> T condition(T cond) {
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		return cond;
	}

	@Test
	public void testSignatureVerifies() {
		assertDoesNotThrow(() -> condition(new ValidateRicalSignature()).execute(env));
	}

	@Test
	public void testSignerCertificateProfile() {
		assertDoesNotThrow(() -> condition(new ValidateRicalSignerCertificate() {
			@Override
			protected Instant now() {
				return FETCHED_AT;
			}
		}).execute(env));
	}

	@Test
	public void testStructureFindingsAreExactlyTheKnownDefects() {
		ValidateRicalStructure cond = condition(new ValidateRicalStructure() {
			@Override
			protected Instant now() {
				return FETCHED_AT;
			}
		});
		assertThrows(ConditionError.class, () -> cond.execute(env));

		Set<Integer> missingIsTrustAnchor = new TreeSet<>();
		Set<Integer> missingAki = new TreeSet<>();
		Set<Integer> misTaggedSerial = new TreeSet<>();
		List<String> unexpected = new ArrayList<>();
		for (String finding : loggedFindings()) {
			if (finding.contains("required field 'isTrustAnchor' is missing")) {
				missingIsTrustAnchor.add(entryIndex(finding));
			} else if (finding.contains("'aki' is missing")) {
				missingAki.add(entryIndex(finding));
			} else if (finding.contains("negative bignum")) {
				misTaggedSerial.add(entryIndex(finding));
			} else {
				unexpected.add(finding);
			}
		}

		assertEquals(List.of(), unexpected, "findings this list is not known to produce");
		assertEquals(ENTRY_COUNT, missingIsTrustAnchor.size(), "every entry omits isTrustAnchor");
		assertEquals(SUB_CA_ENTRIES_WITHOUT_AKI, missingAki);
		assertEquals(Set.of(MIS_TAGGED_SERIAL_ENTRY), misTaggedSerial);
	}

	@Test
	public void testChainUnderCommittedReaderCaIsTrusted() {
		RicalTestFixtures.putSignedRequestObject(env, RicalTestFixtures.readerPkiUnderCommittedCa(
			kotlinInstant(FETCHED_AT.minusSeconds(86_400)), kotlinInstant(FETCHED_AT.plusSeconds(90 * 86_400))));

		assertDoesNotThrow(() -> condition(new ValidateRequestObjectX5cChainAgainstRical() {
			@Override
			protected Instant now() {
				return FETCHED_AT;
			}
		}).execute(env));
		assertDoesNotThrow(() -> condition(new EnsureMatchedRicalEntryHasNoTrustConstraints() {
			@Override
			protected Instant now() {
				return FETCHED_AT;
			}
		}).execute(env));
	}

	@Test
	public void testChainUnderUnlistedCaIsNotTrusted() {
		RicalTestFixtures.putSignedRequestObject(env, RicalTestFixtures.generateReaderPki());

		ConditionError e = assertThrows(ConditionError.class,
			() -> condition(new ValidateRequestObjectX5cChainAgainstRical()).execute(env));
		assertTrue(e.getMessage().contains("does not chain to a reader CA certificate"), e.getMessage());
	}

	private static kotlin.time.Instant kotlinInstant(Instant instant) {
		return kotlin.time.Instant.Companion.fromEpochMilliseconds(instant.toEpochMilli());
	}

	private static int entryIndex(String finding) {
		assertTrue(finding.startsWith("certificateInfos["), finding);
		return Integer.parseInt(finding.substring("certificateInfos[".length(), finding.indexOf(']')));
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private List<String> loggedFindings() {
		ArgumentCaptor<Map> logged = ArgumentCaptor.forClass(Map.class);
		verify(eventLog, atLeastOnce()).log(anyString(), logged.capture());
		List<String> findings = new ArrayList<>();
		for (Map entry : logged.getAllValues()) {
			if (entry.get("findings") instanceof JsonArray array) {
				for (JsonElement finding : array) {
					findings.add(OIDFJSON.getString(finding));
				}
			}
		}
		assertTrue(!findings.isEmpty(), "no findings were logged");
		return findings;
	}
}
