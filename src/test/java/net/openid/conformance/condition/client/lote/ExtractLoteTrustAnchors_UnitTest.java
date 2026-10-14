package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.TestKeysAndCerts;
import net.openid.conformance.util.X509CertificateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

public class ExtractLoteTrustAnchors_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private final LoteTestFixtures.Signer signer = LoteTestFixtures.INSTANCE.generateSigner("EU", "LOTE User", null);
	private ExtractLoteTrustAnchors cond;

	@BeforeEach
	public void setUp() {
		cond = new ExtractLoteTrustAnchors();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
	}

	private JsonArray anchors() {
		return env.getElementFromObject(AbstractLoteCondition.LOTE, "anchors.list").getAsJsonArray();
	}

	private long count(String serviceTypeSuffix) {
		long n = 0;
		for (JsonElement a : anchors()) {
			if (OIDFJSON.getString(a.getAsJsonObject().get("service_type")).endsWith(serviceTypeSuffix)) {
				n++;
			}
		}
		return n;
	}

	@Test
	public void extractsEveryListedCertificateWithItsServiceTypeAndEntityName() throws Exception {
		LoteTestFixtures.ListedCa ca1 = LoteTestFixtures.INSTANCE.generateCa("PID Issuer CA 1", "Example Provider");
		LoteTestFixtures.ListedCa ca2 = LoteTestFixtures.INSTANCE.generateCa("PID Issuer CA 2", "Example Provider");
		LoteTestFixtures.INSTANCE.putParsedLote(env, LoteSlot.PID,
			LoteTestFixtures.INSTANCE.goodPidList(signer, ca1.getCert(), ca2.getCert()));

		cond.execute(env);

		assertThat(anchors()).hasSize(4);
		assertThat(count(LoteSlot.ISSUANCE_SUFFIX)).isEqualTo(2);
		assertThat(count(LoteSlot.REVOCATION_SUFFIX)).isEqualTo(2);
		JsonElement first = anchors().get(0);
		assertThat(OIDFJSON.getString(first.getAsJsonObject().get("te_name"))).isEqualTo("EU PID Provider Trusted Entity");
		assertThat(OIDFJSON.getString(first.getAsJsonObject().get("subject"))).contains("PID Issuer CA 1");
		assertThat(Base64.getDecoder().decode(OIDFJSON.getString(first.getAsJsonObject().get("cert")))).isEqualTo(ca1.getCert().getEncoded());
	}

	@Test
	public void extractsTheAnchorsOfTheEudiwDevelopmentPidList() {
		LoteTestFixtures.INSTANCE.putParsedLote(env, LoteSlot.PID, LoteTestFixtures.INSTANCE.eudiwDevList("PIDProviders"));

		cond.execute(env);

		assertThat(count(LoteSlot.ISSUANCE_SUFFIX)).isEqualTo(7);
		assertThat(count(LoteSlot.REVOCATION_SUFFIX)).isEqualTo(7);
		for (JsonElement a : anchors()) {
			assertThat(OIDFJSON.getString(a.getAsJsonObject().get("subject"))).contains("PID Issuer CA");
		}
	}

	@Test
	public void extractsTheAnchorsOfTheEudiwDevelopmentWrpacList() {
		LoteTestFixtures.INSTANCE.putParsedLote(env, LoteSlot.WRPAC, LoteTestFixtures.INSTANCE.eudiwDevList("WRPACProviders"));

		cond.execute(env);

		assertThat(count(LoteSlot.ISSUANCE_SUFFIX)).isEqualTo(14);
	}

	@Test
	public void extractsTheAnchorsOfTheCommissionsDevelopmentMdlListIncludingTheSuitesIacaRoot() throws Exception {
		// the Commission's mDL providers list, with the suite's IACA root notified under issuance and revocation
		LoteTestFixtures.INSTANCE.putParsedLote(env, LoteSlot.MDL, LoteTestFixtures.INSTANCE.ecDevelopmentList("mdl-providers"));

		cond.execute(env);

		// one provider is listed twice under issuance with two certificates
		assertThat(count(LoteSlot.ISSUANCE_SUFFIX)).isEqualTo(16);
		assertThat(count(LoteSlot.REVOCATION_SUFFIX)).isEqualTo(14);
		byte[] iacaRoot = X509CertUtils.parse(TestKeysAndCerts.IACA_ROOT_CERT_PEM).getEncoded();
		List<String> oidfServiceTypes = new ArrayList<>();
		for (JsonElement a : anchors()) {
			if (Arrays.equals(Base64.getDecoder().decode(OIDFJSON.getString(a.getAsJsonObject().get("cert"))), iacaRoot)) {
				assertThat(OIDFJSON.getString(a.getAsJsonObject().get("te_name"))).isEqualTo(TestKeysAndCerts.ORGANIZATION_NAME);
				oidfServiceTypes.add(OIDFJSON.getString(a.getAsJsonObject().get("service_type")));
			}
		}
		assertThat(oidfServiceTypes).containsExactlyInAnyOrder(LoteTestFixtures.MDL_ISSUANCE, "http://trust.ec.europa.eu/lists/mDL/SvcType/Revocation");
	}

	@Test
	public void skipsServicesOfTypesTheSlotDoesNotAccept() {
		LoteTestFixtures.INSTANCE.putParsedLote(env, LoteSlot.WALLET, LoteTestFixtures.INSTANCE.eudiwDevList("PIDProviders"));

		cond.execute(env);

		assertThat(anchors()).isEmpty();
	}

	@Test
	public void skipsAServiceWhoseStatusSaysItIsNotInForce() {
		LoteTestFixtures.ListedCa withdrawn = LoteTestFixtures.INSTANCE.generateCa("Withdrawn CA", "Former Provider");
		LoteTestFixtures.ListedCa notified = LoteTestFixtures.INSTANCE.generateCa("Notified CA", "Example Provider");
		JsonObject withdrawnService = LoteTestFixtures.INSTANCE.service(LoteTestFixtures.PID_ISSUANCE, "withdrawn issuer", withdrawn.getCert());
		withdrawnService.getAsJsonObject("ServiceInformation").addProperty("ServiceStatus", "http://uri.etsi.org/19602/PIDProvidersList/SvcStatus/withdrawn");
		JsonObject notifiedService = LoteTestFixtures.INSTANCE.service(LoteTestFixtures.PID_ISSUANCE, "notified issuer", notified.getCert());
		notifiedService.getAsJsonObject("ServiceInformation").addProperty("ServiceStatus", "http://uri.etsi.org/19602/PIDProvidersList/SvcStatus/notified");
		LoteTestFixtures.INSTANCE.putParsedLote(env, LoteSlot.PID, LoteTestFixtures.INSTANCE.sign(
			LoteTestFixtures.INSTANCE.payload(LoteTestFixtures.PID_LIST_TYPE,
				List.of(LoteTestFixtures.INSTANCE.trustedEntity("Former Provider", withdrawnService),
					LoteTestFixtures.INSTANCE.trustedEntity("Example Provider", notifiedService)),
				Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600), 1, "EU", "LOTE User", o -> { }),
			signer, true, JWSAlgorithm.ES256));

		cond.execute(env);

		assertThat(anchors()).hasSize(1);
		assertThat(OIDFJSON.getString(anchors().get(0).getAsJsonObject().get("subject"))).contains("Notified CA");
	}

	@Test
	public void aCertificateWhoseOrganizationIsNotTheEntityNameIsStillAnAnchor() {
		LoteTestFixtures.ListedCa ca = LoteTestFixtures.INSTANCE.generateCa("PID Issuer CA", "Example Provider");
		LoteTestFixtures.INSTANCE.putParsedLote(env, LoteSlot.PID, LoteTestFixtures.INSTANCE.sign(
			LoteTestFixtures.INSTANCE.payload(LoteTestFixtures.PID_LIST_TYPE,
				List.of(LoteTestFixtures.INSTANCE.trustedEntity("Example Provider - CZ",
					LoteTestFixtures.INSTANCE.service(LoteTestFixtures.PID_ISSUANCE, "PID issuer", ca.getCert()))),
				Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600), 1, "EU", "LOTE User", o -> { }),
			signer, true, JWSAlgorithm.ES256));

		cond.execute(env);

		assertThat(anchors()).hasSize(1);
		verify(eventLog).log(anyString(), argThat((Map<String, Object> m) -> m.containsKey("mismatches")
			&& m.get("mismatches").toString().contains("Example Provider - CZ")));
	}

	@Test
	public void anEmptyListYieldsNoAnchorsAndNoError() {
		LoteTestFixtures.INSTANCE.putParsedLote(env, LoteSlot.PID, LoteTestFixtures.INSTANCE.emptyPidList(signer));

		cond.execute(env);

		assertThat(anchors()).isEmpty();
	}

	@Test
	public void refusesATamperedList() {
		LoteTestFixtures.ListedCa ca = LoteTestFixtures.INSTANCE.generateCa("PID Issuer CA", "Example Provider");
		String jws = LoteTestFixtures.INSTANCE.goodPidList(signer, ca.getCert());
		String[] parts = jws.split("\\.");
		String tampered = Base64URL.encode(Base64URL.from(parts[1]).decodeToString().replace("PID issuer", "Mallory")).toString();
		LoteTestFixtures.INSTANCE.putParsedLote(env, LoteSlot.PID, parts[0] + "." + tampered + "." + parts[2]);

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class).hasMessageContaining("signature verification failed");
		assertThat(env.getElementFromObject(AbstractLoteCondition.LOTE, "anchors")).isNull();
	}

	@Test
	public void refusesAListNotSealedByTheConfiguredCertificate() {
		LoteTestFixtures.ListedCa ca = LoteTestFixtures.INSTANCE.generateCa("PID Issuer CA", "Example Provider");
		LoteTestFixtures.INSTANCE.putParsedLote(env, LoteSlot.PID, LoteTestFixtures.INSTANCE.goodPidList(signer, ca.getCert()));
		LoteTestFixtures.Signer other = LoteTestFixtures.INSTANCE.generateSigner("EU", "LOTE User", null);
		env.putObject("config", new JsonObject());
		env.putString("config", LoteSlot.SIGNING_CERTIFICATE_CONFIG_KEY, X509CertificateUtil.toPem(other.getCert()));

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class)
			.hasMessageContaining("not sealed by the configured 'LoTE signing certificate'");
		assertThat(env.getElementFromObject(AbstractLoteCondition.LOTE, "anchors")).isNull();

		env.putString("config", LoteSlot.SIGNING_CERTIFICATE_CONFIG_KEY, X509CertificateUtil.toPem(signer.getCert()));
		ExtractLoteTrustAnchors sealedByConfigured = new ExtractLoteTrustAnchors();
		sealedByConfigured.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		sealedByConfigured.execute(env);
		assertThat(anchors()).hasSize(2);
	}

	@Test
	public void deduplicatesACertificateListedTwiceUnderTheSameServiceType() {
		LoteTestFixtures.ListedCa ca = LoteTestFixtures.INSTANCE.generateCa("PID Issuer CA", "Example Provider");
		LoteTestFixtures.INSTANCE.putParsedLote(env, LoteSlot.PID, LoteTestFixtures.INSTANCE.sign(
			LoteTestFixtures.INSTANCE.payload(LoteTestFixtures.PID_LIST_TYPE, List.of(
				LoteTestFixtures.INSTANCE.trustedEntity("A", LoteTestFixtures.INSTANCE.service(LoteTestFixtures.PID_ISSUANCE, "a", ca.getCert())),
				LoteTestFixtures.INSTANCE.trustedEntity("B", LoteTestFixtures.INSTANCE.service(LoteTestFixtures.PID_ISSUANCE, "b", ca.getCert()))),
				java.time.Instant.now().minusSeconds(60), java.time.Instant.now().plusSeconds(3600), 1, "EU", "LOTE User", o -> { }),
			signer, true, com.nimbusds.jose.JWSAlgorithm.ES256));

		cond.execute(env);

		assertThat(anchors()).hasSize(1);
	}
}
