package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.X509CertificateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ValidateLoteSignerAgainstConfiguredCertificate_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private final LoteTestFixtures.Signer signer = LoteTestFixtures.INSTANCE.generateSigner("EU", "LOTE User", null);
	private ValidateLoteSignerAgainstConfiguredCertificate cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateLoteSignerAgainstConfiguredCertificate();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		env.putObject("config", new JsonObject());
		LoteTestFixtures.INSTANCE.putLote(env, LoteSlot.PID, LoteTestFixtures.INSTANCE.emptyPidList(signer));
	}

	@Test
	public void acceptsTheConfiguredCertificateBeingTheSigner() {
		env.putString("config", LoteSlot.SIGNING_CERTIFICATE_CONFIG_KEY, X509CertificateUtil.toPem(signer.getCert()));

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void acceptsASignerThatChainsToTheConfiguredCertificate() {
		LoteTestFixtures.ListedCa sealingCa = LoteTestFixtures.INSTANCE.generateCa("Sealing CA", "LOTE User");
		var leaf = LoteTestFixtures.INSTANCE.mintLeaf(sealingCa, "LoTE sealer");
		LoteTestFixtures.Signer chained = new LoteTestFixtures.Signer(leaf.getFirst(), leaf.getSecond());
		LoteTestFixtures.INSTANCE.putLote(env, LoteSlot.PID, LoteTestFixtures.INSTANCE.emptyPidList(chained));
		env.putString("config", LoteSlot.SIGNING_CERTIFICATE_CONFIG_KEY, X509CertificateUtil.toPem(sealingCa.getCert()));

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void acceptsAFullChainThatCarriesTheConfiguredCertificate() {
		LoteTestFixtures.ListedCa sealingCa = LoteTestFixtures.INSTANCE.generateCa("Sealing CA", "LOTE User");
		var leaf = LoteTestFixtures.INSTANCE.mintLeaf(sealingCa, "LoTE sealer");

		assertThatCode(() -> ValidateLoteSignerAgainstConfiguredCertificate.checkSigner(
			List.of(leaf.getSecond(), sealingCa.getCert()), sealingCa.getCert())).doesNotThrowAnyException();
	}

	@Test
	public void passesWhenNoCertificateIsConfigured() {
		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void passesWhenTheConfiguredFieldIsBlank() {
		env.putString("config", LoteSlot.SIGNING_CERTIFICATE_CONFIG_KEY, "");

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void rejectsASignerUnrelatedToTheConfiguredCertificate() {
		LoteTestFixtures.Signer other = LoteTestFixtures.INSTANCE.generateSigner("EU", "LOTE User", null);
		env.putString("config", LoteSlot.SIGNING_CERTIFICATE_CONFIG_KEY, X509CertificateUtil.toPem(other.getCert()));

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class)
			.hasMessageContaining("neither the configured LoTE signing certificate nor chains to it");
	}

	@Test
	public void rejectsAConfiguredValueThatIsNotACertificate() {
		env.putString("config", LoteSlot.SIGNING_CERTIFICATE_CONFIG_KEY, "-----BEGIN CERTIFICATE-----\nnope\n-----END CERTIFICATE-----");

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class)
			.hasMessageContaining("'LoTE signing certificate' field");
	}
}
