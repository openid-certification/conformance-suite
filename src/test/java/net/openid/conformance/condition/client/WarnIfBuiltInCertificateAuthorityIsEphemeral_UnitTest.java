package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The warnings about ephemeral stand-in CAs. The suite's CA identities come from the process
 * environment, so the "ephemeral" cases assume the test JVM has no {@code SUITE_CA_KEY_*} variables,
 * as CI does not; a developer running with the keys set gets those cases skipped.
 */
public class WarnIfBuiltInCertificateAuthorityIsEphemeral_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	@BeforeEach
	public void setUp() {
		env.putObject("config", new JsonObject());
	}

	private <T extends AbstractCondition> T cond(T cond) {
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.WARNING);
		return cond;
	}

	private static void assumeNoKeyFor(TestKeysAndCerts.SuiteCa ca) {
		Assumptions.assumeTrue(System.getenv(TestKeysAndCerts.keyEnvironmentVariable(ca.getSlug())) == null,
			"the published CA's key is in the environment, so it is not ephemeral here");
	}

	private static JsonObject someJwks() {
		return JsonParser.parseString("{\"keys\":[{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"a\",\"y\":\"b\"}]}").getAsJsonObject();
	}

	@Test
	public void theAccessCertificateWarningNamesTheAccessAndWrprcCasAndTheirVariables() {
		assumeNoKeyFor(TestKeysAndCerts.SuiteCa.ACCESS);
		assumeNoKeyFor(TestKeysAndCerts.SuiteCa.WRPRC_PROVIDER);

		assertThatThrownBy(() -> cond(new WarnIfBuiltInAccessCertificateIsEphemeral()).execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("SUITE_CA_KEY_ACCESS_CA")
			.hasMessageContaining("SUITE_CA_KEY_WRPRC_PROVIDER_CA");
	}

	@Test
	public void aConfiguredVerifierInfoLeavesOnlyTheAccessCertificateBuiltIn() {
		assumeNoKeyFor(TestKeysAndCerts.SuiteCa.ACCESS);
		env.putObject("config", "client.verifier_info", new JsonObject());

		assertThatThrownBy(() -> cond(new WarnIfBuiltInAccessCertificateIsEphemeral()).execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("SUITE_CA_KEY_ACCESS_CA")
			.satisfies(e -> org.assertj.core.api.Assertions.assertThat(e.getMessage()).doesNotContain("WRPRC"));
	}

	@Test
	public void aConfiguredClientJwksMeansNoBuiltInRequestSigner() {
		env.putObject("config", "client.jwks", someJwks());

		assertThatCode(() -> cond(new WarnIfBuiltInAccessCertificateIsEphemeral()).execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void theCredentialSignerWarningNamesThePidProviderCa() {
		assumeNoKeyFor(TestKeysAndCerts.SuiteCa.PID_PROVIDER);

		assertThatThrownBy(() -> cond(new WarnIfBuiltInCredentialSignerIsEphemeral()).execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("SUITE_CA_KEY_PID_PROVIDER_CA")
			.hasMessageContaining("'Signing JWK' in the 'Credential Issuer' section");
	}

	@Test
	public void theMetadataSignerWarningNamesTheAccessCa() {
		assumeNoKeyFor(TestKeysAndCerts.SuiteCa.ACCESS);

		assertThatThrownBy(() -> cond(new WarnIfBuiltInMetadataSignerIsEphemeral()).execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("SUITE_CA_KEY_ACCESS_CA")
			.hasMessageContaining("credential issuer metadata");
	}

	@Test
	public void aConfiguredSigningJwkMeansNoBuiltInMetadataSigner() {
		env.putObject("config", "credential.signing_jwk", someJwks());

		assertThatCode(() -> cond(new WarnIfBuiltInMetadataSignerIsEphemeral()).execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void aConfiguredSigningJwkMeansNoBuiltInCredentialSigner() {
		env.putObject("config", "credential.signing_jwk", someJwks());

		assertThatCode(() -> cond(new WarnIfBuiltInCredentialSignerIsEphemeral()).execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void theAttestationSignerWarningsNameTheWalletProviderCa() {
		assumeNoKeyFor(TestKeysAndCerts.SuiteCa.WALLET_PROVIDER);

		assertThatThrownBy(() -> cond(new WarnIfBuiltInClientAttestationSignerIsEphemeral()).execute(env))
			.isInstanceOf(ConditionError.class).hasMessageContaining("SUITE_CA_KEY_WALLET_PROVIDER_CA");
		assertThatThrownBy(() -> cond(new WarnIfBuiltInKeyAttestationSignerIsEphemeral()).execute(env))
			.isInstanceOf(ConditionError.class).hasMessageContaining("SUITE_CA_KEY_WALLET_PROVIDER_CA");
	}

	@Test
	public void configuredAttestationKeysIncludingTheLegacyFieldsMeanNoBuiltInSigner() {
		env.putObject("config", "vci.client_attester_keys_jwks", someJwks());
		env.putObject("config", "client_attestation.key_attestation_jwks", someJwks());

		assertThatCode(() -> cond(new WarnIfBuiltInClientAttestationSignerIsEphemeral()).execute(env)).doesNotThrowAnyException();
		assertThatCode(() -> cond(new WarnIfBuiltInKeyAttestationSignerIsEphemeral()).execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void aClearedFieldCountsAsMissing() {
		assumeNoKeyFor(TestKeysAndCerts.SuiteCa.PID_PROVIDER);
		env.putString("config", "credential.signing_jwk", "");

		assertThatThrownBy(() -> cond(new WarnIfBuiltInCredentialSignerIsEphemeral()).execute(env)).isInstanceOf(ConditionError.class);
	}
}
