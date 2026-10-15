package net.openid.conformance.condition.client.lote;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ValidateLoteSignerCertificate_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private ValidateLoteSignerCertificate cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateLoteSignerCertificate();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.WARNING);
	}

	private void putList(LoteTestFixtures.Signer signer, String territory, String operator) {
		String jws = LoteTestFixtures.INSTANCE.sign(
			LoteTestFixtures.INSTANCE.payload(LoteTestFixtures.PID_LIST_TYPE, null, Instant.now().minusSeconds(60),
				Instant.now().plusSeconds(3600), 1, territory, operator, o -> { }),
			signer, true, com.nimbusds.jose.JWSAlgorithm.ES256);
		LoteTestFixtures.INSTANCE.putParsedLote(env, LoteSlot.PID, jws);
	}

	@Test
	public void acceptsASignerNamingTheSchemeTerritoryAndOperator() {
		putList(LoteTestFixtures.INSTANCE.generateSigner("EU", "LOTE User", null), "EU", "LOTE User");

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void flagsTheEudiwDevelopmentListWhoseSignerIsNotItsSchemeOperator() {
		// the development list is sealed by "O=Niscy" while its SchemeOperatorName is "LOTE User": a real-world
		// mismatch, which is why this check is called as a WARNING and the anchors are still used
		LoteTestFixtures.INSTANCE.putParsedLote(env, LoteSlot.PID, LoteTestFixtures.INSTANCE.eudiwDevList("PIDProviders"));

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class)
			.hasMessageContaining("6.8.0");
	}

	@Test
	public void rejectsACountryThatIsNotTheSchemeTerritory() {
		putList(LoteTestFixtures.INSTANCE.generateSigner("DE", "LOTE User", null), "EU", "LOTE User");

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class).hasMessageContaining("6.8.0");
	}

	@Test
	public void rejectsAnOrganizationThatIsNotASchemeOperatorName() {
		putList(LoteTestFixtures.INSTANCE.generateSigner("EU", "Somebody Else", null), "EU", "LOTE User");

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class).hasMessageContaining("6.8.0");
	}

	@Test
	public void rejectsAnExpiredSigner() {
		putList(LoteTestFixtures.INSTANCE.generateSigner("EU", "LOTE User", Instant.now().minusSeconds(60)), "EU", "LOTE User");

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class).hasMessageContaining("6.8.0");
	}
}
