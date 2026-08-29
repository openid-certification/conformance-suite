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
		// negative-bignum tag, which multipaz's strict parser rejects; the lenient parse
		// must keep the rest of the list usable for the trust evaluation
		RicalTestFixtures.putSignedRequestObject(env, pki);
		RicalTestFixtures.ReaderPki brokenEntryPki = RicalTestFixtures.generateReaderPki("Mis-encoded Reader CA");
		RicalTestFixtures.putRical(env, RicalTestFixtures.sign(RicalTestFixtures.buildRicalMap(List.of(
			RicalTestFixtures.certificateInfo(pki.getCaCert()),
			RicalTestFixtures.certificateInfo(brokenEntryPki.getCaCert(), true, null,
				java.util.Set.of(), java.util.Map.of(), true)))));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenRicalSignatureBroken() {
		RicalTestFixtures.putSignedRequestObject(env, pki);
		byte[] rical = RicalTestFixtures.goodSignedRical(List.of(pki.getCaCert()));
		rical[rical.length / 2] ^= 0x01;
		RicalTestFixtures.putRical(env, rical);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("could not be parsed or its COSE signature"), e.getMessage());
	}
}
