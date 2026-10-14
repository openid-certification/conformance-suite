package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
	public void skipsServicesOfTypesTheSlotDoesNotAccept() {
		LoteTestFixtures.INSTANCE.putParsedLote(env, LoteSlot.WALLET, LoteTestFixtures.INSTANCE.eudiwDevList("PIDProviders"));

		cond.execute(env);

		assertThat(anchors()).isEmpty();
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

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class).hasMessageContaining("cannot be used");
		assertThat(env.getElementFromObject(AbstractLoteCondition.LOTE, "anchors")).isNull();
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
