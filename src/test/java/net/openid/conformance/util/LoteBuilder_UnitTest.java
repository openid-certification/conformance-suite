package net.openid.conformance.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.lote.AbstractLoteCondition;
import net.openid.conformance.condition.client.lote.ExtractLoteTrustAnchors;
import net.openid.conformance.condition.client.lote.LoteSlot;
import net.openid.conformance.condition.client.lote.ParseLote;
import net.openid.conformance.condition.client.lote.ValidateLoteSignature;
import net.openid.conformance.condition.client.lote.ValidateLoteSignerAgainstConfiguredCertificate;
import net.openid.conformance.condition.client.lote.ValidateLoteSignerCertificate;
import net.openid.conformance.condition.client.lote.ValidateLoteStructure;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The suite's own lists of trusted entities are sealed with the LoTE signer, carry the suite's
 * CAs under the right service types, and pass every check the suite applies to a scheme
 * operator's list.
 */
public class LoteBuilder_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private static LoteSlot slotFor(LoteList list) {
		return switch (list) {
			case PID_PROVIDERS -> LoteSlot.PID;
			case MDL_PROVIDERS -> LoteSlot.MDL;
			case WALLET_PROVIDERS -> LoteSlot.WALLET;
			case WRPAC_PROVIDERS -> LoteSlot.WRPAC;
			case WRPRC_PROVIDERS -> LoteSlot.WRPRC;
		};
	}

	private <T extends AbstractCondition> void run(T cond, Environment env) {
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		assertThatCode(() -> cond.execute(env)).as(cond.getClass().getSimpleName()).doesNotThrowAnyException();
	}

	private Environment envWith(LoteList list, boolean empty) {
		Environment env = new Environment();
		JsonObject config = new JsonObject();
		env.putObject("config", config);
		env.putString("config", LoteSlot.SIGNING_CERTIFICATE_CONFIG_KEY, TestKeysAndCerts.getLoteSignerCertPem());
		JsonObject lote = new JsonObject();
		lote.addProperty("slot", slotFor(list).name());
		lote.addProperty("url", LoteBuilder.url(list, empty));
		lote.addProperty("value", LoteBuilder.build(list, empty));
		env.putObject(AbstractLoteCondition.LOTE, lote);
		return env;
	}

	@ParameterizedTest
	@EnumSource(LoteList.class)
	public void everyListPassesTheSuitesOwnLoteChecks(LoteList list) {
		Environment env = envWith(list, false);
		run(new ParseLote(), env);
		run(new ValidateLoteSignature(), env);
		run(new ValidateLoteSignerCertificate(), env);
		run(new ValidateLoteSignerAgainstConfiguredCertificate(), env);
		run(new ValidateLoteStructure(), env);
		run(new ExtractLoteTrustAnchors(), env);

		JsonArray anchors = env.getElementFromObject(AbstractLoteCondition.LOTE, "anchors.list").getAsJsonArray();
		assertThat(anchors).hasSize(list.services.size());
		for (int i = 0; i < list.services.size(); i++) {
			JsonObject anchor = anchors.get(i).getAsJsonObject();
			assertThat(OIDFJSON.getString(anchor.get("service_type"))).isEqualTo(list.services.get(i).serviceType());
			assertThat(OIDFJSON.getString(anchor.get("te_name"))).isEqualTo(TestKeysAndCerts.ORGANIZATION_NAME);
			assertThat(OIDFJSON.getString(anchor.get("subject"))).isEqualTo(list.services.get(i).ca().getCert().getSubject().getName());
		}
	}

	@ParameterizedTest
	@EnumSource(LoteList.class)
	public void everyEmptyListPassesTheChecksAndYieldsNoAnchors(LoteList list) {
		Environment env = envWith(list, true);
		run(new ParseLote(), env);
		run(new ValidateLoteSignature(), env);
		run(new ValidateLoteStructure(), env);
		run(new ExtractLoteTrustAnchors(), env);

		assertThat(env.getElementFromObject(AbstractLoteCondition.LOTE, "anchors.list").getAsJsonArray()).isEmpty();
		JsonElement payload = env.getElementFromObject(AbstractLoteCondition.LOTE, "payload");
		assertThat(payload.getAsJsonObject().getAsJsonObject("LoTE").has("TrustedEntitiesList")).isFalse();
	}

	@Test
	public void theHeaderMirrorsTheEudiwReferenceLists() throws Exception {
		JWSObject jws = JWSObject.parse(LoteBuilder.build(LoteList.PID_PROVIDERS, false));
		assertThat(jws.getHeader().getAlgorithm().getName()).isEqualTo("ES256");
		assertThat(jws.getHeader().getType().getType()).isEqualTo("JOSE+JSON");
		assertThat(jws.getHeader().getX509CertChain()).hasSize(1);
		assertThat(jws.getHeader().getX509CertChain().get(0).decode()).isEqualTo(TestKeysAndCerts.getLoteSignerCert().getEncoded());
		assertThat(jws.getHeader().getX509CertSHA256Thumbprint())
			.isEqualTo(X509CertUtils.computeSHA256Thumbprint(TestKeysAndCerts.getLoteSignerCert()));
		assertThat(jws.getHeader().getKeyID()).isNotBlank();
		assertThat(jws.getHeader().getCustomParam("iat")).isNotNull();
		assertThat(jws.verify(new ECDSAVerifier(TestKeysAndCerts.getLoteSignerJwk().toECPublicKey()))).isTrue();
	}

	@Test
	public void theSchemeInformationFollowsTheProfiles() {
		Instant issued = Instant.parse("2026-10-14T12:00:00Z");
		JsonObject info = LoteBuilder.payload(LoteList.WRPAC_PROVIDERS, false, issued)
			.getAsJsonObject("LoTE").getAsJsonObject("ListAndSchemeInformation");
		assertThat(OIDFJSON.getInt(info.get("LoTEVersionIdentifier"))).isEqualTo(1);
		assertThat(OIDFJSON.getLong(info.get("LoTESequenceNumber"))).isEqualTo(issued.getEpochSecond());
		assertThat(OIDFJSON.getString(info.get("LoTEType"))).isEqualTo(LoteList.WRPAC_PROVIDERS.loteType);
		assertThat(OIDFJSON.getString(info.get("ListIssueDateTime"))).isEqualTo("2026-10-14T12:00:00Z");
		assertThat(OIDFJSON.getString(info.get("NextUpdate"))).isEqualTo("2026-10-15T12:00:00Z");
		assertThat(OIDFJSON.getString(info.get("SchemeTerritory"))).isEqualTo(LoteBuilder.SCHEME_TERRITORY);
		assertThat(info.getAsJsonArray("DistributionPoints")).hasSize(1);
		assertThat(OIDFJSON.getString(info.getAsJsonArray("DistributionPoints").get(0))).isEqualTo(LoteBuilder.url(LoteList.WRPAC_PROVIDERS, false));
		JsonObject pointer = info.getAsJsonArray("PointersToOtherLoTE").get(0).getAsJsonObject();
		assertThat(OIDFJSON.getString(pointer.get("LoTELocation"))).isEqualTo(LoteBuilder.url(LoteList.WRPAC_PROVIDERS, false));
	}

	@ParameterizedTest
	@EnumSource(value = LoteList.class, names = {"PID_PROVIDERS", "MDL_PROVIDERS", "WALLET_PROVIDERS", "WRPAC_PROVIDERS"})
	public void everyListedCaHasAnIssuanceAndARevocationServiceAndNoTwoServicesOfATypeShareACa(LoteList list) {
		// the eIDAS Dashboard's notification rules for the Launchpad event
		for (LoteList.Service service : list.services) {
			assertThat(list.services).anyMatch(s -> s.ca() == service.ca() && s.serviceType().endsWith("/Issuance"));
			assertThat(list.services).anyMatch(s -> s.ca() == service.ca() && s.serviceType().endsWith("/Revocation"));
			assertThat(list.services).filteredOn(s -> s.ca() == service.ca() && s.serviceType().equals(service.serviceType())).hasSize(1);
		}
	}

	@Test
	public void theListsNameTheRightCas() {
		List<TestKeysAndCerts.SuiteCa> pidCas = LoteList.PID_PROVIDERS.services.stream().map(LoteList.Service::ca).toList();
		assertThat(pidCas).contains(TestKeysAndCerts.SuiteCa.MDOC_IACA_ROOT, TestKeysAndCerts.SuiteCa.PID_PROVIDER);
		assertThat(LoteList.MDL_PROVIDERS.services).extracting(LoteList.Service::ca).containsOnly(TestKeysAndCerts.SuiteCa.MDOC_IACA_ROOT);
		assertThat(LoteList.WALLET_PROVIDERS.services).extracting(LoteList.Service::ca).containsOnly(TestKeysAndCerts.SuiteCa.WALLET_PROVIDER);
		assertThat(LoteList.WRPAC_PROVIDERS.services).extracting(LoteList.Service::ca).containsOnly(TestKeysAndCerts.SuiteCa.ACCESS);
		assertThat(LoteList.WRPRC_PROVIDERS.services).extracting(LoteList.Service::ca).containsOnly(TestKeysAndCerts.SuiteCa.WRPRC_PROVIDER);
	}
}
