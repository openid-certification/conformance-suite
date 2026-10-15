package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

public class ValidateLoteStructure_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private final LoteTestFixtures.Signer signer = LoteTestFixtures.INSTANCE.generateSigner("EU", "LOTE User", null);
	private final LoteTestFixtures.ListedCa ca = LoteTestFixtures.INSTANCE.generateCa("PID Issuer CA", "Example Provider");
	private ValidateLoteStructure cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateLoteStructure();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.WARNING);
	}

	private JsonObject goodEntity() {
		return LoteTestFixtures.INSTANCE.trustedEntity("EU PID Provider Trusted Entity",
			LoteTestFixtures.INSTANCE.service(LoteTestFixtures.PID_ISSUANCE, "PID issuer", ca.getCert()),
			LoteTestFixtures.INSTANCE.service(LoteTestFixtures.PID_REVOCATION, "PID revocation", ca.getCert()));
	}

	private void put(LoteSlot slot, String loteType, List<JsonObject> entities, Instant issued, Instant nextUpdate, int version,
			Consumer<JsonObject> mutate) {
		JsonObject payload = LoteTestFixtures.INSTANCE.payload(loteType, entities, issued, nextUpdate, version, "EU", "LOTE User", mutate);
		LoteTestFixtures.INSTANCE.putParsedLote(env, slot, LoteTestFixtures.INSTANCE.sign(payload, signer, true, com.nimbusds.jose.JWSAlgorithm.ES256));
	}

	private void putGood(Consumer<JsonObject> mutate) {
		put(LoteSlot.PID, LoteTestFixtures.PID_LIST_TYPE, List.of(goodEntity()), Instant.now().minusSeconds(60),
			Instant.now().plusSeconds(30L * 24 * 3600), 1, mutate);
	}

	private static JsonObject info(JsonObject root) {
		return root.getAsJsonObject("LoTE").getAsJsonObject("ListAndSchemeInformation");
	}

	private static JsonObject firstService(JsonObject root) {
		return root.getAsJsonObject("LoTE").getAsJsonArray("TrustedEntitiesList").get(0).getAsJsonObject()
			.getAsJsonArray("TrustedEntityServices").get(0).getAsJsonObject();
	}

	@Test
	public void acceptsAWellFormedList() {
		putGood(o -> { });

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void acceptsAnEmptyListWithoutTrustedEntitiesList() {
		put(LoteSlot.PID, LoteTestFixtures.PID_LIST_TYPE, null, Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600), 1, o -> { });

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void mdlSlotAcceptsAnyIssuanceServiceType() {
		put(LoteSlot.MDL, "http://trust.ec.europa.eu/lists/mDL/mDLProvidersListType",
			List.of(LoteTestFixtures.INSTANCE.trustedEntity("mDL provider",
				LoteTestFixtures.INSTANCE.service("http://uri.etsi.org/19602/SvcType/mDL/Issuance", "mDL issuer", ca.getCert()))),
			Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600), 1, o -> { });

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void flagsAVersionOtherThanOne() {
		put(LoteSlot.PID, LoteTestFixtures.PID_LIST_TYPE, List.of(goodEntity()), Instant.now().minusSeconds(60),
			Instant.now().plusSeconds(3600), 2, o -> { });

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class).hasMessageContaining("structural problem");
		assertFinding("LoTEVersionIdentifier");
	}

	@Test
	public void flagsAListTypeTheSlotDoesNotExpect() {
		put(LoteSlot.PID, LoteTestFixtures.WALLET_LIST_TYPE, List.of(goodEntity()), Instant.now().minusSeconds(60),
			Instant.now().plusSeconds(3600), 1, o -> { });

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
		assertFinding("LoTEType");
	}

	@Test
	public void flagsAnOverdueNextUpdate() {
		put(LoteSlot.PID, LoteTestFixtures.PID_LIST_TYPE, List.of(goodEntity()), Instant.now().minusSeconds(7200),
			Instant.now().minusSeconds(3600), 1, o -> { });

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
		assertFinding("NextUpdate");
		assertFinding("6.3.15");
	}

	@Test
	public void flagsANextUpdateMoreThanSixMonthsAfterIssue() {
		put(LoteSlot.PID, LoteTestFixtures.PID_LIST_TYPE, List.of(goodEntity()), Instant.now().minusSeconds(60),
			Instant.now().plusSeconds(200L * 24 * 3600), 1, o -> { });

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
		assertFinding("six months");
	}

	@Test
	public void flagsAnIssueDateInTheFuture() {
		put(LoteSlot.PID, LoteTestFixtures.PID_LIST_TYPE, List.of(goodEntity()), Instant.now().plusSeconds(3600),
			Instant.now().plusSeconds(7200), 1, o -> { });

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
		assertFinding("ListIssueDateTime");
	}

	@Test
	public void flagsAServiceStatus() {
		putGood(o -> firstService(o).getAsJsonObject("ServiceInformation").addProperty("ServiceStatus", "http://uri.etsi.org/TrstSvc/TrustedList/Svcstatus/granted"));

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
		assertFinding("ServiceStatus");
	}

	@Test
	public void flagsAServiceTypeTheProfileDoesNotAllow() {
		putGood(o -> firstService(o).getAsJsonObject("ServiceInformation").addProperty("ServiceTypeIdentifier", LoteTestFixtures.WRPAC_ISSUANCE));

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
		assertFinding("ServiceTypeIdentifier");
	}

	@Test
	public void flagsAServiceWithoutServiceInformationWrapper() {
		putGood(o -> {
			JsonObject service = firstService(o);
			JsonObject info = service.remove("ServiceInformation").getAsJsonObject();
			info.entrySet().forEach(e -> service.add(e.getKey(), e.getValue()));
		});

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
		assertFinding("ServiceInformation is missing");
	}

	@Test
	public void flagsAServiceWithoutCertificates() {
		putGood(o -> firstService(o).getAsJsonObject("ServiceInformation").getAsJsonObject("ServiceDigitalIdentity")
			.add("X509Certificates", new JsonArray()));

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
		assertFinding("X509Certificates");
	}

	@Test
	public void flagsACertificateThatDoesNotParse() {
		putGood(o -> firstService(o).getAsJsonObject("ServiceInformation").getAsJsonObject("ServiceDigitalIdentity")
			.getAsJsonArray("X509Certificates").get(0).getAsJsonObject().addProperty("val", "AAAA"));

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
		assertFinding("not a base64 DER X.509 certificate");
	}

	@Test
	public void flagsAnEmptyTrustedEntitiesListArray() {
		put(LoteSlot.PID, LoteTestFixtures.PID_LIST_TYPE, List.of(), Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600), 1, o -> { });

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
		assertFinding("6.4.0");
	}

	@Test
	public void flagsMissingSchemeInformationFields() {
		putGood(o -> {
			info(o).remove("SchemeTerritory");
			info(o).remove("SchemeOperatorName");
			info(o).remove("StatusDeterminationApproach");
		});

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
		assertFinding("SchemeTerritory");
		assertFinding("SchemeOperatorName");
		assertFinding("StatusDeterminationApproach");
	}

	/** The findings are logged in the failure entry's 'findings' detail, so assertions capture the logged map. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private void assertFinding(String text) {
		ArgumentCaptor<Map> logged = ArgumentCaptor.forClass(Map.class);
		verify(eventLog, atLeastOnce()).log(anyString(), logged.capture());
		StringBuilder findings = new StringBuilder();
		for (Map entry : logged.getAllValues()) {
			findings.append(entry.get("findings")).append('\n');
		}
		assertThat(findings.toString()).contains(text);
	}
}
