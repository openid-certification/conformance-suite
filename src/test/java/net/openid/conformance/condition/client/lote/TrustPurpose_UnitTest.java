package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TrustPurpose_UnitTest {

	private final Environment env = new Environment();

	@BeforeEach
	public void setUp() {
		env.putObject("config", new JsonObject());
	}

	@Test
	public void theLegacyKeyIsReadWhenTheCurrentOneIsAbsent() {
		env.putString("config", "vci.client_attestation_trust_anchor", "legacy");

		assertThat(TrustPurpose.CLIENT_ATTESTATION.configuredPem(env)).isEqualTo("legacy");
	}

	@Test
	public void theCurrentKeyWinsOverTheLegacyOne() {
		env.putString("config", "client_attestation.trust_anchor", "current");
		env.putString("config", "vci.client_attestation_trust_anchor", "legacy");

		assertThat(TrustPurpose.CLIENT_ATTESTATION.configuredPem(env)).isEqualTo("current");
	}

	@Test
	public void aClearedFieldIsNotConfigured() {
		env.putString("config", "credential.trust_anchor_pem", " ");

		assertThat(TrustPurpose.CREDENTIAL.configuredPem(env)).isNull();
		assertThat(TrustPurpose.CREDENTIAL.configuredSources(env)).isEmpty();
	}

	@Test
	public void registeredPemsLandWhereTheChainChecksReadThem() {
		TrustPurpose.KEY_ATTESTATION.register(env, "key");
		TrustPurpose.REQUEST_OBJECT.register(env, "request");

		assertThat(env.getString("key_attestation_trust_anchor_pem")).isEqualTo("key");
		assertThat(env.getString("client_request_object_trust_anchor_pem")).isEqualTo("request");
		assertThat(TrustPurpose.KEY_ATTESTATION.registeredPem(env)).isEqualTo("key");
	}

	@Test
	public void configuredSourcesNameThePemAndTheLists() {
		env.putString("config", "credential.trust_anchor_pem", "pem");
		env.putString("config", LoteSlot.MDL.configKey, "https://lote.example.com/mdl.jwt");

		assertThat(TrustPurpose.CREDENTIAL.configuredSources(env)).containsExactly(
			"'Credential Trust Anchor' field in the 'Credential Issuer' section of the test configuration",
			LoteSlot.MDL.fieldDescription());
	}
}
