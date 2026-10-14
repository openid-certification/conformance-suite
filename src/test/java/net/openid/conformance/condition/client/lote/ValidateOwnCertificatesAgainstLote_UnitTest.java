package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The pre-flight checks pass when the suite's own signing certificates (built-in or configured)
 * chain to an entity in the configured list, and report which certificate is not covered when
 * they do not.
 */
public class ValidateOwnCertificatesAgainstLote_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private final LoteTestFixtures.ListedCa strangerCa = LoteTestFixtures.INSTANCE.generateCa("Stranger CA", "Stranger");

	@BeforeEach
	public void setUp() {
		env.putObject("config", new JsonObject());
	}

	private <T extends AbstractCondition> T cond(T cond) {
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.WARNING);
		return cond;
	}

	private static X509Certificate javaCert(TestKeysAndCerts.SuiteCa ca) throws Exception {
		return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(ca.getCertPem().getBytes()));
	}

	// --- request signing (VP wallet tests) ---

	@Test
	public void builtInRequestSignerPassesWhenTheAccessCaIsListed() throws Exception {
		env.putObject("client_jwks", TestKeysAndCerts.getWrpac().jwksJson());
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.WRPAC, LoteTestFixtures.WRPAC_ISSUANCE, "OpenID Foundation", javaCert(TestKeysAndCerts.SuiteCa.ACCESS));

		assertThatCode(() -> cond(new ValidateOwnRequestSigningCertificateAgainstWrpacLote()).execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void requestSignerIsReportedWhenTheListDoesNotCoverIt() throws Exception {
		env.putObject("client_jwks", TestKeysAndCerts.getWrpac().jwksJson());
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.WRPAC, LoteTestFixtures.WRPAC_ISSUANCE, "Stranger", strangerCa.getCert());

		assertThatThrownBy(() -> cond(new ValidateOwnRequestSigningCertificateAgainstWrpacLote()).execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'WRPAC providers LoTE URL'")
			.hasMessageContaining("'jwks' field in the 'Client' section");
	}

	@Test
	public void aSecondClientsKeyIsCheckedToo() throws Exception {
		env.putObject("client_jwks", TestKeysAndCerts.getWrpac().jwksJson());
		var strangerLeaf = LoteTestFixtures.INSTANCE.mintLeaf(strangerCa, "second client");
		env.putObject("client2_jwks", JsonParser.parseString("{\"keys\":[" + strangerLeaf.getFirst().toJSONString() + "]}").getAsJsonObject());
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.WRPAC, LoteTestFixtures.WRPAC_ISSUANCE, "OpenID Foundation", javaCert(TestKeysAndCerts.SuiteCa.ACCESS));

		assertThatThrownBy(() -> cond(new ValidateOwnRequestSigningCertificateAgainstWrpacLote()).execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'Second client'");
	}

	// --- credential signing (VP verifier and VCI wallet tests) ---

	@Test
	public void builtInCredentialSignersPassWhenTheIacaAndPidProviderCaAreListed() throws Exception {
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.PID, LoteTestFixtures.PID_ISSUANCE, "OpenID Foundation",
			javaCert(TestKeysAndCerts.SuiteCa.MDOC_IACA_ROOT), javaCert(TestKeysAndCerts.SuiteCa.PID_PROVIDER));

		assertThatCode(() -> cond(new ValidateOwnCredentialSigningCertificatesAgainstLotes()).execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void theMdlListCoversTheMdocSignerWhileThePidListCoversTheSdJwtSigner() throws Exception {
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.MDL, LoteTestFixtures.MDL_ISSUANCE, "OpenID Foundation",
			javaCert(TestKeysAndCerts.SuiteCa.MDOC_IACA_ROOT));
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.PID, LoteTestFixtures.PID_ISSUANCE, "OpenID Foundation",
			javaCert(TestKeysAndCerts.SuiteCa.PID_PROVIDER));

		assertThatCode(() -> cond(new ValidateOwnCredentialSigningCertificatesAgainstLotes()).execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void aConfiguredSigningJwkIsCheckedInsteadOfTheBuiltInSdJwtSigner() throws Exception {
		var configured = LoteTestFixtures.INSTANCE.mintLeaf(strangerCa, "configured signer");
		env.putObject("config", "credential.signing_jwk", JsonParser.parseString(configured.getFirst().toJSONString()).getAsJsonObject());
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.PID, LoteTestFixtures.PID_ISSUANCE, "OpenID Foundation",
			javaCert(TestKeysAndCerts.SuiteCa.MDOC_IACA_ROOT), javaCert(TestKeysAndCerts.SuiteCa.PID_PROVIDER));

		assertThatThrownBy(() -> cond(new ValidateOwnCredentialSigningCertificatesAgainstLotes()).execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'Signing JWK'");
	}

	@Test
	public void theMdocSignerIsReportedWhenOnlyThePidProviderCaIsListed() throws Exception {
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.PID, LoteTestFixtures.PID_ISSUANCE, "OpenID Foundation",
			javaCert(TestKeysAndCerts.SuiteCa.PID_PROVIDER));

		assertThatThrownBy(() -> cond(new ValidateOwnCredentialSigningCertificatesAgainstLotes()).execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("built-in mdoc document signer");
	}

	@Test
	public void credentialCheckDoesNothingWithoutAnyList() {
		assertThatCode(() -> cond(new ValidateOwnCredentialSigningCertificatesAgainstLotes()).execute(env)).doesNotThrowAnyException();
	}

	// --- attestation signing (VCI issuer tests) ---

	@Test
	public void builtInAttestationSignerPassesWhenTheWalletProviderCaIsListed() throws Exception {
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.WALLET, LoteTestFixtures.WALLET_ISSUANCE, "OpenID Foundation",
			javaCert(TestKeysAndCerts.SuiteCa.WALLET_PROVIDER));

		assertThatCode(() -> cond(new ValidateOwnAttestationSigningCertificateAgainstWalletLote()).execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void aConfiguredAttesterIsCheckedInsteadOfTheBuiltInOne() throws Exception {
		var configured = LoteTestFixtures.INSTANCE.mintLeaf(strangerCa, "configured attester");
		env.putObject("config", "client_attestation.attester_jwks",
			JsonParser.parseString("{\"keys\":[" + configured.getFirst().toJSONString() + "]}").getAsJsonObject());
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.WALLET, LoteTestFixtures.WALLET_ISSUANCE, "Stranger", strangerCa.getCert());

		assertThatCode(() -> cond(new ValidateOwnAttestationSigningCertificateAgainstWalletLote()).execute(env)).doesNotThrowAnyException();
		assertThat(env.getObject(LoteSlot.WALLET.envKey)).isNotNull();
	}

	@Test
	public void slotConstantsMatchTheEnvKeys() {
		assertThat(LoteSlot.PID_ENV_KEY).isEqualTo(LoteSlot.PID.envKey);
		assertThat(LoteSlot.MDL_ENV_KEY).isEqualTo(LoteSlot.MDL.envKey);
		assertThat(LoteSlot.WALLET_ENV_KEY).isEqualTo(LoteSlot.WALLET.envKey);
		assertThat(LoteSlot.WRPAC_ENV_KEY).isEqualTo(LoteSlot.WRPAC.envKey);
	}
}
