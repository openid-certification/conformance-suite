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
import org.multipaz.cbor.CborArray;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class EnsureMatchedRicalEntryHasNoTrustConstraints_UnitTest {

	private EnsureMatchedRicalEntryHasNoTrustConstraints cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private RicalTestFixtures.ReaderPki pki;

	@BeforeEach
	public void setUp() {
		cond = new EnsureMatchedRicalEntryHasNoTrustConstraints();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
		pki = RicalTestFixtures.generateReaderPki();
		RicalTestFixtures.putSignedRequestObject(env, pki);
	}

	@Test
	public void testEvaluate_passesWhenMatchedEntryHasNoConstraints() {
		RicalTestFixtures.putRical(env, RicalTestFixtures.goodSignedRical(List.of(pki.getCaCert())));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenMatchedEntryCarriesConstraints() {
		// a TrustConstraint may be an empty map (only the optional extensions field is defined)
		DataItem constraint = new CborMap(new LinkedHashMap<>(), false);
		DataItem constraints = new CborArray(new ArrayList<>(List.of(constraint)), false);
		RicalTestFixtures.putRical(env, RicalTestFixtures.sign(RicalTestFixtures.buildRicalMap(List.of(
			RicalTestFixtures.certificateInfo(pki.getCaCert(), true, null, Set.of(),
				Map.of("trustConstraints", constraints))))));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("trust constraints"), e.getMessage());
	}

	@Test
	public void testEvaluate_noErrorWhenNoEntryMatches() {
		RicalTestFixtures.ReaderPki otherPki = RicalTestFixtures.generateReaderPki("Unrelated Reader CA");
		RicalTestFixtures.putRical(env, RicalTestFixtures.goodSignedRical(List.of(otherPki.getCaCert())));

		// an untrusted chain is the chain check's finding, not this condition's
		assertDoesNotThrow(() -> cond.execute(env));
	}
}
