package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
public class VCIEnsureSingleProofWhenBatchIssuanceNotAdvertised_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private VCIEnsureSingleProofWhenBatchIssuanceNotAdvertised cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureSingleProofWhenBatchIssuanceNotAdvertised();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	private void putProofCount(int count) {
		JsonArray items = new JsonArray();
		for (int i = 0; i < count; i++) {
			items.add(new JsonObject());
		}
		JsonObject wrapper = new JsonObject();
		wrapper.add("items", items);
		env.putObject("proof_jwts", wrapper);
	}

	private void putMetadata(Integer batchSize) {
		JsonObject metadata = new JsonObject();
		if (batchSize != null) {
			JsonObject batch = new JsonObject();
			batch.addProperty("batch_size", batchSize);
			metadata.add("batch_credential_issuance", batch);
		}
		env.putObject("credential_issuer_metadata", metadata);
	}

	@Test
	public void noBatch_oneProof_passes() {
		putMetadata(null);
		putProofCount(1);
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void noBatch_twoProofs_fails() {
		putMetadata(null);
		putProofCount(2);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void batchAdvertised_manyProofs_passes() {
		putMetadata(10);
		putProofCount(5);
		assertDoesNotThrow(() -> cond.execute(env));
	}
}
