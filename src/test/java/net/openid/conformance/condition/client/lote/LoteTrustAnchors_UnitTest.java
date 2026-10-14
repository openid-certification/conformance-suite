package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.X509CertificateUtil;
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
	public void unionsTheConfiguredPemWithTheSlotsAnchorsOfTheWantedServiceType() throws Exception {
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.PID, LoteTestFixtures.PID_ISSUANCE, "PID entity", pidCa.getCert());
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.PID, LoteTestFixtures.PID_REVOCATION, "PID entity", revocationCa.getCert());
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.MDL, LoteTestFixtures.MDL_ISSUANCE, "mDL entity", mdlCa.getCert());

		LoteTrustAnchors.TrustAnchorSet issuance = LoteTrustAnchors.resolve(env, X509CertificateUtil.toPem(configuredCa.getCert()),
			"Credential Trust Anchor", LoteSlot.ISSUANCE_SUFFIX, LoteSlot.PID, LoteSlot.MDL);
		assertThat(issuance.anchors()).containsExactly(configuredCa.getCert(), pidCa.getCert(), mdlCa.getCert());
		assertThat(issuance.sources().get(0)).isEqualTo("'Credential Trust Anchor' from the test configuration");
		assertThat(issuance.sources().get(1)).startsWith("PID providers LoTE URL: PID entity, PID/Issuance, ");
		assertThat(issuance.sourceOf(mdlCa.getCert())).startsWith("mDL providers LoTE URL: mDL entity, mDL/Issuance, ");

		LoteTrustAnchors.TrustAnchorSet revocation = LoteTrustAnchors.resolve(env, null, "Status List Trust Anchor",
			LoteSlot.REVOCATION_SUFFIX, LoteSlot.PID, LoteSlot.MDL);
		assertThat(revocation.anchors()).containsExactly(revocationCa.getCert());
	}

	@Test
	public void shortensBothStylesOfServiceTypeToTheListAndTheService() {
		assertThat(LoteTrustAnchors.shortServiceType(LoteTestFixtures.PID_ISSUANCE)).isEqualTo("PID/Issuance");
		assertThat(LoteTrustAnchors.shortServiceType(LoteTestFixtures.MDL_ISSUANCE)).isEqualTo("mDL/Issuance");
	}

	@Test
	public void isEmptyWithNothingConfiguredAndNoSlots() throws Exception {
		LoteTrustAnchors.TrustAnchorSet set = LoteTrustAnchors.resolve(env, null, "Credential Trust Anchor",
			LoteSlot.ISSUANCE_SUFFIX, LoteSlot.PID, LoteSlot.MDL);
		assertThat(set.isEmpty()).isTrue();
		assertThat(set.trustsNothing()).isFalse();
		assertThat(set.sourcesJson()).isEmpty();
		assertThat(LoteTrustAnchors.anySlotHasAnchors(env, LoteSlot.PID, LoteSlot.MDL)).isFalse();
	}

	@Test
	public void aConfiguredListWithoutAnchorsOfTheWantedTypeTrustsNothing() throws Exception {
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.PID, LoteTestFixtures.PID_REVOCATION, "PID entity", revocationCa.getCert());

		LoteTrustAnchors.TrustAnchorSet set = LoteTrustAnchors.resolve(env, null, "Credential Trust Anchor",
			LoteSlot.ISSUANCE_SUFFIX, LoteSlot.PID, LoteSlot.MDL);
		assertThat(set.isEmpty()).isTrue();
		assertThat(set.trustsNothing()).isTrue();
		assertThat(set.configuredLists()).containsExactly(LoteSlot.PID.fieldDescription());
	}

	@Test
	public void aConfiguredPemKeepsAnEmptyListFromTrustingNothing() throws Exception {
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.PID, LoteTestFixtures.PID_ISSUANCE, "PID entity");

		LoteTrustAnchors.TrustAnchorSet set = LoteTrustAnchors.resolve(env, X509CertificateUtil.toPem(configuredCa.getCert()),
			"Credential Trust Anchor", LoteSlot.ISSUANCE_SUFFIX, LoteSlot.PID);
		assertThat(set.trustsNothing()).isFalse();
		assertThat(set.anchors()).containsExactly(configuredCa.getCert());
	}

	@Test
	public void deduplicatesACertificateListedInSeveralSlots() throws Exception {
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.PID, LoteTestFixtures.PID_ISSUANCE, "PID entity", pidCa.getCert());
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.MDL, LoteTestFixtures.PID_ISSUANCE, "mDL entity", pidCa.getCert());

		LoteTrustAnchors.TrustAnchorSet set = LoteTrustAnchors.resolve(env, X509CertificateUtil.toPem(pidCa.getCert()),
			"Credential Trust Anchor", LoteSlot.ISSUANCE_SUFFIX, LoteSlot.PID, LoteSlot.MDL);
		assertThat(set.anchors()).containsExactly(pidCa.getCert());
	}

	@Test
	public void rejectsAConfiguredPemThatIsNotACertificate() {
		assertThatThrownBy(() -> LoteTrustAnchors.resolve(env, "not a pem", "Credential Trust Anchor", LoteSlot.ISSUANCE_SUFFIX, LoteSlot.PID))
			.isInstanceOf(X509CertificateUtil.X5cCertificateChainException.class);
	}

	@Test
	public void anySlotConfiguredLooksAtTheConfigurationNotTheFetchedLists() {
		env.putObject("config", new JsonObject());
		assertThat(LoteTrustAnchors.anySlotConfigured(env, LoteSlot.PID, LoteSlot.MDL)).isFalse();
		env.putString("config", LoteSlot.MDL.configKey, "https://lote.example.com/mdl.jwt");
		assertThat(LoteTrustAnchors.anySlotConfigured(env, LoteSlot.PID, LoteSlot.MDL)).isTrue();
		env.putString("config", LoteSlot.MDL.configKey, "");
		assertThat(LoteTrustAnchors.anySlotConfigured(env, LoteSlot.PID, LoteSlot.MDL)).isFalse();
	}
}
