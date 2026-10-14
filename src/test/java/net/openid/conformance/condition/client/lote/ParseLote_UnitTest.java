package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ParseLote_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private final LoteTestFixtures.Signer signer = LoteTestFixtures.INSTANCE.generateSigner("EU", "LOTE User", null);
	private ParseLote cond;

	@BeforeEach
	public void setUp() {
		cond = new ParseLote();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
	}

	@Test
	public void parsesAGeneratedListAndStoresHeaderAndPayload() {
		LoteTestFixtures.ListedCa ca = LoteTestFixtures.INSTANCE.generateCa("PID Issuer CA", "Example Provider");
		LoteTestFixtures.INSTANCE.putLote(env, LoteSlot.PID, LoteTestFixtures.INSTANCE.goodPidList(signer, ca.getCert()));

		cond.execute(env);

		JsonObject header = env.getElementFromObject(AbstractLoteCondition.LOTE, "header").getAsJsonObject();
		assertThat(OIDFJSON.getString(header.get("alg"))).isEqualTo("ES256");
		assertThat(header.has("x5c")).isTrue();
		JsonObject payload = env.getElementFromObject(AbstractLoteCondition.LOTE, "payload").getAsJsonObject();
		assertThat(OIDFJSON.getString(payload.getAsJsonObject("LoTE").getAsJsonObject("ListAndSchemeInformation").get("LoTEType")))
			.isEqualTo(LoteTestFixtures.PID_LIST_TYPE);
	}

	@ParameterizedTest
	@ValueSource(strings = {"PIDProviders", "WalletProviders", "WRPACProviders", "WRPRCProviders", "PubEAAProviders"})
	public void parsesTheEudiwDevelopmentLists(String name) {
		LoteTestFixtures.INSTANCE.putLote(env, LoteSlot.PID, LoteTestFixtures.INSTANCE.eudiwDevList(name));

		cond.execute(env);

		JsonObject payload = env.getElementFromObject(AbstractLoteCondition.LOTE, "payload").getAsJsonObject();
		assertThat(payload.getAsJsonObject("LoTE").getAsJsonArray("TrustedEntitiesList")).hasSize(1);
	}

	@Test
	public void acceptsAnEmptyListWithoutTrustedEntitiesList() {
		LoteTestFixtures.INSTANCE.putLote(env, LoteSlot.PID, LoteTestFixtures.INSTANCE.emptyPidList(signer));

		cond.execute(env);

		JsonObject payload = env.getElementFromObject(AbstractLoteCondition.LOTE, "payload").getAsJsonObject();
		assertThat(payload.getAsJsonObject("LoTE").has("TrustedEntitiesList")).isFalse();
	}

	@Test
	public void rejectsSomethingThatIsNotACompactJws() {
		LoteTestFixtures.INSTANCE.putLote(env, LoteSlot.PID, "<html>not a list</html>");

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class).hasMessageContaining("compact JWS");
	}

	@Test
	public void rejectsAPayloadWithoutTheLoteObject() {
		JsonObject root = new JsonObject();
		root.addProperty("hello", "world");
		LoteTestFixtures.INSTANCE.putLote(env, LoteSlot.PID, LoteTestFixtures.INSTANCE.sign(root, signer, true, com.nimbusds.jose.JWSAlgorithm.ES256));

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class).hasMessageContaining("'LoTE' object");
	}
}
