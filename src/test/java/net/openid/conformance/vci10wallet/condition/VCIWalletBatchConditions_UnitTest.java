package net.openid.conformance.vci10wallet.condition;

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
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VCIWalletBatchConditions_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private VCIEnsureProofCountWithinAdvertisedBatchSize countCond;

	private VCIEnsureProofKeysAreDistinct distinctCond;

	private VCIReverseCredentialIssuanceOrder reverseCond;

	@BeforeEach
	public void setUp() {
		countCond = new VCIEnsureProofCountWithinAdvertisedBatchSize();
		countCond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		distinctCond = new VCIEnsureProofKeysAreDistinct();
		distinctCond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		reverseCond = new VCIReverseCredentialIssuanceOrder();
		reverseCond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private void putAdvertisedBatchSize(int batchSize) {
		JsonObject batch = new JsonObject();
		batch.addProperty("batch_size", batchSize);
		JsonObject metadata = new JsonObject();
		metadata.add("batch_credential_issuance", batch);
		env.putObject("credential_issuer_metadata", metadata);
	}

	private void putProofJwtsWithKeys(ECKey... keys) {
		JsonArray items = new JsonArray();
		for (ECKey key : keys) {
			JsonObject item = new JsonObject();
			item.add("jwk", JsonParser.parseString(key.toPublicJWK().toJSONString()));
			items.add(item);
		}
		JsonObject wrapper = new JsonObject();
		wrapper.add("items", items);
		env.putObject("proof_jwts", wrapper);
	}

	private void putProofCount(int count) throws Exception {
		ECKey[] keys = new ECKey[count];
		for (int i = 0; i < count; i++) {
			keys[i] = new ECKeyGenerator(Curve.P_256).generate();
		}
		putProofJwtsWithKeys(keys);
	}

	private void putCredentials(String... credentials) {
		JsonArray array = new JsonArray();
		for (String credential : credentials) {
			JsonObject obj = new JsonObject();
			obj.addProperty("credential", credential);
			array.add(obj);
		}
		JsonObject issuance = new JsonObject();
		issuance.add("credentials", array);
		env.putObject("credential_issuance", issuance);
	}

	@Test
	public void testCount_passesAtBatchSize() throws Exception {
		putAdvertisedBatchSize(3);
		putProofCount(3);

		assertDoesNotThrow(() -> countCond.execute(env));
	}

	@Test
	public void testCount_failsAboveBatchSize() throws Exception {
		putAdvertisedBatchSize(3);
		putProofCount(4);

		assertThrows(ConditionError.class, () -> countCond.execute(env));
	}

	@Test
	public void testDistinct_passesWithDistinctKeys() throws Exception {
		putProofJwtsWithKeys(
			new ECKeyGenerator(Curve.P_256).generate(),
			new ECKeyGenerator(Curve.P_256).generate());

		assertDoesNotThrow(() -> distinctCond.execute(env));
	}

	@Test
	public void testDistinct_failsWithDuplicateKey() throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).generate();
		putProofJwtsWithKeys(key, new ECKeyGenerator(Curve.P_256).generate(), key);

		assertThrows(ConditionError.class, () -> distinctCond.execute(env));
	}

	@Test
	public void testReverse_reversesCredentialOrder() {
		putCredentials("first", "second", "third");

		assertDoesNotThrow(() -> reverseCond.execute(env));

		JsonArray credentials = env.getObject("credential_issuance").getAsJsonArray("credentials");
		assertEquals("third", OIDFJSON.getString(credentials.get(0).getAsJsonObject().get("credential")));
		assertEquals("second", OIDFJSON.getString(credentials.get(1).getAsJsonObject().get("credential")));
		assertEquals("first", OIDFJSON.getString(credentials.get(2).getAsJsonObject().get("credential")));
	}

	@Test
	public void testReverse_singleCredentialIsNoOp() {
		putCredentials("only");

		assertDoesNotThrow(() -> reverseCond.execute(env));

		JsonArray credentials = env.getObject("credential_issuance").getAsJsonArray("credentials");
		assertEquals(1, credentials.size());
		assertEquals("only", OIDFJSON.getString(credentials.get(0).getAsJsonObject().get("credential")));
	}
}
