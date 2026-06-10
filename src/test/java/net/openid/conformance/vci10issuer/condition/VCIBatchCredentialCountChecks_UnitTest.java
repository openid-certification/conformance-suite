package net.openid.conformance.vci10issuer.condition;

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
public class VCIBatchCredentialCountChecks_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private VCIEnsureNotMoreCredentialsThanRequestedProofs notMoreCond;

	private VCIWarnIfFewerCredentialsThanRequestedProofs fewerCond;

	@BeforeEach
	public void setUp() {
		notMoreCond = new VCIEnsureNotMoreCredentialsThanRequestedProofs();
		notMoreCond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		fewerCond = new VCIWarnIfFewerCredentialsThanRequestedProofs();
		fewerCond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	private void setupCounts(int credentials, int requested) {
		JsonArray list = new JsonArray();
		for (int i = 0; i < credentials; i++) {
			list.add("credential" + i);
		}
		JsonObject extracted = new JsonObject();
		extracted.add("list", list);
		env.putObject("extracted_credentials", extracted);
		env.putInteger("vci_batch_requested_proof_count", requested);
	}

	@Test
	public void testNotMore_passesWhenEqual() {
		setupCounts(5, 5);
		assertDoesNotThrow(() -> notMoreCond.execute(env));
	}

	@Test
	public void testNotMore_passesWhenFewer() {
		setupCounts(3, 5);
		assertDoesNotThrow(() -> notMoreCond.execute(env));
	}

	@Test
	public void testNotMore_failsWhenMore() {
		setupCounts(6, 5);
		assertThrows(ConditionError.class, () -> notMoreCond.execute(env));
	}

	@Test
	public void testFewer_passesWhenEqual() {
		setupCounts(5, 5);
		assertDoesNotThrow(() -> fewerCond.execute(env));
	}

	@Test
	public void testFewer_warnsWhenFewer() {
		setupCounts(3, 5);
		assertThrows(ConditionError.class, () -> fewerCond.execute(env));
	}

	@Test
	public void testFewer_passesWhenMore() {
		// the not-more condition reports this case; this condition only covers 'fewer'
		setupCounts(6, 5);
		assertDoesNotThrow(() -> fewerCond.execute(env));
	}
}
