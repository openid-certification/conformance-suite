package net.openid.conformance.vci10issuer.condition;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VCIValidateBatchCredentialIssuanceMetadata_UnitTest {

	private VCIValidateBatchCredentialIssuanceMetadata cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIValidateBatchCredentialIssuanceMetadata();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private void putBatchCredentialIssuance(JsonObject batchCredentialIssuance) {
		env.putObject("vci", "credential_issuer_metadata.batch_credential_issuance", batchCredentialIssuance);
	}

	@Test
	public void testEvaluate_acceptsBatchSizeOfTwo() {
		JsonObject batch = new JsonObject();
		batch.addProperty("batch_size", 2);
		putBatchCredentialIssuance(batch);

		assertDoesNotThrow(() -> cond.execute(env));
		assertEquals(2, env.getInteger("vci_batch_size"));
	}

	@Test
	public void testEvaluate_acceptsLargeBatchSize() {
		JsonObject batch = new JsonObject();
		batch.addProperty("batch_size", 50);
		putBatchCredentialIssuance(batch);

		assertDoesNotThrow(() -> cond.execute(env));
		assertEquals(50, env.getInteger("vci_batch_size"));
	}

	@Test
	public void testEvaluate_rejectsBatchSizeOfOne() {
		JsonObject batch = new JsonObject();
		batch.addProperty("batch_size", 1);
		putBatchCredentialIssuance(batch);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsMissingBatchSize() {
		putBatchCredentialIssuance(new JsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsStringBatchSize() {
		JsonObject batch = new JsonObject();
		batch.addProperty("batch_size", "5");
		putBatchCredentialIssuance(batch);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsNonIntegralBatchSize() {
		JsonObject batch = new JsonObject();
		batch.addProperty("batch_size", 2.5);
		putBatchCredentialIssuance(batch);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsMissingBatchCredentialIssuance() {
		env.putObject("vci", new JsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
