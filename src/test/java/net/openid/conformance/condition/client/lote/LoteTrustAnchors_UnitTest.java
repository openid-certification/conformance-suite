package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.X509CertificateUtil;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LoteTrustAnchors_UnitTest {

	private final Environment env = new Environment();
	private final LoteTestFixtures.ListedCa pidCa = LoteTestFixtures.INSTANCE.generateCa("PID CA", "PID Org");
	private final LoteTestFixtures.ListedCa revocationCa = LoteTestFixtures.INSTANCE.generateCa("Revocation CA", "PID Org");
	private final LoteTestFixtures.ListedCa mdlCa = LoteTestFixtures.INSTANCE.generateCa("mDL CA", "mDL Org");
	private final LoteTestFixtures.ListedCa configuredCa = LoteTestFixtures.INSTANCE.generateCa("Configured CA", "Tester");

	@Test
	public void collectsTheSlotsAnchorsOfTheWantedServiceType() throws Exception {
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.PID, LoteTestFixtures.PID_ISSUANCE, "PID entity", pidCa.getCert());
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.PID, LoteTestFixtures.PID_REVOCATION, "PID entity", revocationCa.getCert());
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.MDL, LoteTestFixtures.MDL_ISSUANCE, "mDL entity", mdlCa.getCert());

		LoteTrustAnchors.TrustAnchorSet issuance = LoteTrustAnchors.resolve(env, null,
			"Credential Trust Anchor", LoteSlot.ISSUANCE_SUFFIX, List.of(LoteSlot.PID, LoteSlot.MDL));
		assertThat(issuance.certs()).containsExactly(pidCa.getCert(), mdlCa.getCert());
		assertThat(issuance.sources().get(0)).startsWith("PID providers LoTE URL: PID entity, PID/Issuance, ");
		assertThat(issuance.sourceOf(mdlCa.getCert())).startsWith("mDL providers LoTE URL: mDL entity, mDL/Issuance, ");

		LoteTrustAnchors.TrustAnchorSet revocation = LoteTrustAnchors.resolve(env, null, "Status List Trust Anchor",
			LoteSlot.REVOCATION_SUFFIX, List.of(LoteSlot.PID, LoteSlot.MDL));
		assertThat(revocation.certs()).containsExactly(revocationCa.getCert());
	}

	@Test
	public void shortensBothStylesOfServiceTypeToTheListAndTheService() {
		assertThat(LoteTrustAnchors.shortServiceType(LoteTestFixtures.PID_ISSUANCE)).isEqualTo("PID/Issuance");
		assertThat(LoteTrustAnchors.shortServiceType(LoteTestFixtures.MDL_ISSUANCE)).isEqualTo("mDL/Issuance");
	}

	@Test
	public void isEmptyWithNothingConfiguredAndNoSlots() throws Exception {
		LoteTrustAnchors.TrustAnchorSet set = LoteTrustAnchors.resolve(env, null, "Credential Trust Anchor",
			LoteSlot.ISSUANCE_SUFFIX, List.of(LoteSlot.PID, LoteSlot.MDL));
		assertThat(set.isEmpty()).isTrue();
		assertThat(set.trustsNothing()).isFalse();
		assertThat(set.sourcesJson()).isEmpty();
	}

	@Test
	public void aConfiguredListWithoutAnchorsOfTheWantedTypeTrustsNothing() throws Exception {
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.PID, LoteTestFixtures.PID_REVOCATION, "PID entity", revocationCa.getCert());

		LoteTrustAnchors.TrustAnchorSet set = LoteTrustAnchors.resolve(env, null, "Credential Trust Anchor",
			LoteSlot.ISSUANCE_SUFFIX, List.of(LoteSlot.PID, LoteSlot.MDL));
		assertThat(set.isEmpty()).isTrue();
		assertThat(set.trustsNothing()).isTrue();
		assertThat(set.configuredLists()).containsExactly(LoteSlot.PID.fieldDescription());
		assertThat(set.unusableLists()).isEmpty();
		assertThat(set.trustsNothingMessage("the chain")).contains("lists no trust anchor for the chain");
	}

	@Test
	public void aConfiguredListWhoseSetupFailedTrustsNothingAndSaysSo() throws Exception {
		// RegisterLote created the slot object, but nothing after it stored anchors
		JsonObject registered = new JsonObject();
		registered.addProperty("slot", LoteSlot.PID.name());
		registered.addProperty("url", "https://lote.example.com/pid.jwt");
		env.putObject(LoteSlot.PID.envKey, registered);

		LoteTrustAnchors.TrustAnchorSet set = LoteTrustAnchors.resolve(env, null, "Credential Trust Anchor",
			LoteSlot.ISSUANCE_SUFFIX, List.of(LoteSlot.PID, LoteSlot.MDL));
		assertThat(set.trustsNothing()).isTrue();
		assertThat(set.unusableLists()).containsExactly(LoteSlot.PID.fieldDescription());
		assertThat(set.trustsNothingMessage("the chain"))
			.contains(LoteSlot.PID.fieldDescription())
			.contains("could not be fetched or checked");
	}

	@Test
	public void aConfiguredPemIsTheOnlyAnchorAndTheListsAreNotConsulted() throws Exception {
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.PID, LoteTestFixtures.PID_ISSUANCE, "PID entity", pidCa.getCert());
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.MDL, LoteTestFixtures.PID_ISSUANCE, "mDL entity");

		LoteTrustAnchors.TrustAnchorSet set = LoteTrustAnchors.resolve(env, X509CertificateUtil.toPem(configuredCa.getCert()),
			"Credential Trust Anchor", LoteSlot.ISSUANCE_SUFFIX, List.of(LoteSlot.PID, LoteSlot.MDL));
		assertThat(set.certs()).containsExactly(configuredCa.getCert());
		assertThat(set.sources()).containsExactly("'Credential Trust Anchor' from the test configuration");
		assertThat(set.trustsNothing()).isFalse();
	}

	@Test
	public void deduplicatesACertificateListedInSeveralSlots() throws Exception {
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.PID, LoteTestFixtures.PID_ISSUANCE, "PID entity", pidCa.getCert());
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.MDL, LoteTestFixtures.PID_ISSUANCE, "mDL entity", pidCa.getCert());

		LoteTrustAnchors.TrustAnchorSet set = LoteTrustAnchors.resolve(env, null,
			"Credential Trust Anchor", LoteSlot.ISSUANCE_SUFFIX, List.of(LoteSlot.PID, LoteSlot.MDL));
		assertThat(set.certs()).containsExactly(pidCa.getCert());
	}

	@Test
	public void configuredSourcesListsThePemFieldThenEachConfiguredList() {
		env.putObject("config", new JsonObject());
		assertThat(LoteTrustAnchors.configuredSources(env, null, "the PEM field", List.of(LoteSlot.PID, LoteSlot.MDL))).isEmpty();

		env.putString("config", LoteSlot.MDL.configKey, "https://lote.example.com/mdl.jwt");
		env.putString("config", LoteSlot.PID.configKey, "");
		assertThat(LoteTrustAnchors.configuredSources(env, " ", "the PEM field", List.of(LoteSlot.PID, LoteSlot.MDL)))
			.containsExactly(LoteSlot.MDL.fieldDescription());
		assertThat(LoteTrustAnchors.configuredSources(env, "-----BEGIN CERTIFICATE-----", "the PEM field", List.of(LoteSlot.PID, LoteSlot.MDL)))
			.containsExactly("the PEM field", LoteSlot.MDL.fieldDescription());
	}

	@Test
	public void rejectsAConfiguredPemThatIsNotACertificate() {
		assertThatThrownBy(() -> LoteTrustAnchors.resolve(env, "not a pem", "Credential Trust Anchor", LoteSlot.ISSUANCE_SUFFIX, List.of(LoteSlot.PID)))
			.isInstanceOf(X509CertificateUtil.X5cCertificateChainException.class);
	}

	@Test
	public void configuredSourcesLooksAtTheConfigurationNotTheFetchedLists() {
		env.putObject("config", new JsonObject());
		env.putObject(LoteSlot.PID.envKey, new JsonObject());
		assertThat(LoteTrustAnchors.configuredSources(env, null, "PEM", List.of(LoteSlot.PID, LoteSlot.MDL))).isEmpty();
		env.putString("config", LoteSlot.MDL.configKey, "https://lote.example.com/mdl.jwt");
		assertThat(LoteTrustAnchors.configuredSources(env, null, "PEM", List.of(LoteSlot.PID, LoteSlot.MDL))).containsExactly(LoteSlot.MDL.fieldDescription());
		env.putString("config", LoteSlot.MDL.configKey, "");
		assertThat(LoteTrustAnchors.configuredSources(env, null, "PEM", List.of(LoteSlot.PID, LoteSlot.MDL))).isEmpty();
	}
}
