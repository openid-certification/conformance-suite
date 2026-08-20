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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateMdocIssuerChainAgainstTrustAnchor_UnitTest {

	private ValidateMdocIssuerChainAgainstTrustAnchor cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private VicalTestFixtures.IssuerPki pki;

	@BeforeEach
	public void setUp() {
		cond = new ValidateMdocIssuerChainAgainstTrustAnchor();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
		pki = VicalTestFixtures.generateIssuerPki();
		MdocCredentialTestUtil.putCredential(env,
			VicalTestFixtures.issuerSignedFromPki(pki, "org.iso.18013.5.1.mDL"));
	}

	@Test
	public void testEvaluate_passesWhenChainValidatesToAnchor() {
		env.putString("credential_trust_anchor_pem", VicalTestFixtures.toPem(pki.getIacaCert()));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenChainDoesNotValidateToAnchor() {
		VicalTestFixtures.IssuerPki otherPki = VicalTestFixtures.generateIssuerPki();
		env.putString("credential_trust_anchor_pem", VicalTestFixtures.toPem(otherPki.getIacaCert()));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().toLowerCase().contains("path validation")
			|| e.getMessage().toLowerCase().contains("chain"), e.getMessage());
	}

	@Test
	public void testEvaluate_skipsWhenVicalConfigured() {
		// a configured VICAL takes precedence; even a non-matching anchor must not fail
		VicalTestFixtures.IssuerPki otherPki = VicalTestFixtures.generateIssuerPki();
		env.putString("credential_trust_anchor_pem", VicalTestFixtures.toPem(otherPki.getIacaCert()));
		VicalTestFixtures.putVical(env,
			VicalTestFixtures.goodSignedVical(List.of(pki.getIacaCert())));

		assertDoesNotThrow(() -> cond.execute(env));
	}
}
