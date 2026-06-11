package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VCIBatchBindingKeyChecks_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private VCIEnsureBatchBindingKeysMatchSentProofKeys matchCond;

	private VCIEnsureBatchBindingKeysAreDistinct distinctCond;

	private ECKey key1;

	private ECKey key2;

	@BeforeEach
	public void setUp() throws Exception {
		matchCond = new VCIEnsureBatchBindingKeysMatchSentProofKeys();
		matchCond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		distinctCond = new VCIEnsureBatchBindingKeysAreDistinct();
		distinctCond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
		key1 = new ECKeyGenerator(Curve.P_256).generate();
		key2 = new ECKeyGenerator(Curve.P_256).generate();
	}

	private void putBindingKeys(ECKey... keys) {
		JsonArray array = new JsonArray();
		for (ECKey key : keys) {
			array.add(JsonParser.parseString(key.toPublicJWK().toJSONString()));
		}
		JsonObject bindingKeys = new JsonObject();
		bindingKeys.add("keys", array);
		env.putObject("vci_batch_binding_keys", bindingKeys);
	}

	private void putProofKeys(ECKey... keys) {
		JsonArray array = new JsonArray();
		for (ECKey key : keys) {
			array.add(JsonParser.parseString(key.toPublicJWK().toJSONString()));
		}
		JsonObject jwks = new JsonObject();
		jwks.add("keys", array);
		env.putObject("vci_batch_proof_public_jwks", jwks);
	}

	@Test
	public void testMatch_passesWhenAllBindingKeysWereSent() {
		putBindingKeys(key1, key2);
		putProofKeys(key1, key2);

		assertDoesNotThrow(() -> matchCond.execute(env));
	}

	@Test
	public void testMatch_passesWhenSubsetOfSentKeysUsed() {
		putBindingKeys(key1);
		putProofKeys(key1, key2);

		assertDoesNotThrow(() -> matchCond.execute(env));
	}

	@Test
	public void testMatch_failsWhenBindingKeyWasNeverSent() throws Exception {
		ECKey foreignKey = new ECKeyGenerator(Curve.P_256).generate();
		putBindingKeys(key1, foreignKey);
		putProofKeys(key1, key2);

		assertThrows(ConditionError.class, () -> matchCond.execute(env));
	}

	@Test
	public void testDistinct_passesWhenAllKeysDiffer() {
		putBindingKeys(key1, key2);

		assertDoesNotThrow(() -> distinctCond.execute(env));
	}

	@Test
	public void testDistinct_failsWhenKeyReused() {
		putBindingKeys(key1, key2, key1);

		assertThrows(ConditionError.class, () -> distinctCond.execute(env));
	}
}
