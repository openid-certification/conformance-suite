package net.openid.conformance.condition.as.clientattestation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.util.Base64;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.lote.LoteSlot;
import net.openid.conformance.condition.client.lote.LoteTestFixtures;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.X509CertificateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The attestation's x5c chain may end at the configured 'Client Attestation Trust Anchor' or at
 * any wallet provider listed in the configured list of trusted entities.
 */
public class ValidateClientAttestationX5cClaimInProofJwt_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private final LoteTestFixtures.ListedCa walletProviderCa = LoteTestFixtures.INSTANCE.generateCa("Wallet Provider CA", "Wallet Org");
	private final LoteTestFixtures.ListedCa otherCa = LoteTestFixtures.INSTANCE.generateCa("Other CA", "Other Org");
	private ValidateClientAttestationX5cClaimInProofJwt cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateClientAttestationX5cClaimInProofJwt();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		env.putObject("vci", new JsonObject());
		JsonArray x5c = new JsonArray();
		x5c.add(Base64.encode(LoteTestFixtures.INSTANCE.mintLeaf(walletProviderCa, "attester").getSecond().getEncoded()).toString());
		JsonObject header = new JsonObject();
		header.add("x5c", x5c);
		JsonObject attestation = new JsonObject();
		attestation.add("header", header);
		env.putObject("client_attestation_object", attestation);
	}

	@Test
	public void passesWhenTheChainEndsAtAListedWalletProvider() {
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.WALLET, LoteTestFixtures.WALLET_ISSUANCE, "Wallet entity", walletProviderCa.getCert());

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void passesWhenTheChainEndsAtTheConfiguredAnchor() {
		env.putString("vci", "client_attestation_trust_anchor_pem", X509CertificateUtil.toPem(walletProviderCa.getCert()));

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void failsWhenNoAnchorIsAvailableAtAll() {
		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class)
			.hasMessageContaining("'Wallet providers LoTE URL'");
	}

	@Test
	public void failsWhenTheListIsConfiguredButListsNoWalletProvider() {
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.WALLET, LoteTestFixtures.WALLET_ISSUANCE, "Wallet entity");

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class)
			.hasMessageContaining("No trust anchor for the client attestation");
	}

	@Test
	public void failsWhenTheChainEndsAtNoneOfTheAnchors() {
		env.putString("vci", "client_attestation_trust_anchor_pem", X509CertificateUtil.toPem(otherCa.getCert()));
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.WALLET, LoteTestFixtures.WALLET_ISSUANCE, "Wallet entity", otherCa.getCert());

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class).hasMessageContaining("PKIX path validation failed");
	}
}
