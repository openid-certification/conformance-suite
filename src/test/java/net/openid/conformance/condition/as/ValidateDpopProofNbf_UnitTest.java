package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateDpopProofNbf_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateDpopProofNbf cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateDpopProofNbf();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void addDpopProof(long nbf) {
		JsonObject claims = new JsonObject();
		claims.addProperty("nbf", nbf);
		JsonObject proof = new JsonObject();
		proof.add("claims", claims);
		env.putObject("incoming_dpop_proof", proof);
	}

	@Test
	public void testNbfRecent() {
		addDpopProof(Instant.now().getEpochSecond() - 10);
		cond.execute(env);
	}

	@Test
	public void testNbfTooOld() {
		assertThrows(ConditionError.class, () -> {
			addDpopProof(Instant.now().getEpochSecond() - 600);
			cond.execute(env);
		});
	}

	@Test
	public void testNbfInFuture() {
		assertThrows(ConditionError.class, () -> {
			addDpopProof(Instant.now().getEpochSecond() + 600);
			cond.execute(env);
		});
	}

	@Test
	public void testNoNbf() {
		JsonObject claims = new JsonObject();
		JsonObject proof = new JsonObject();
		proof.add("claims", claims);
		env.putObject("incoming_dpop_proof", proof);
		cond.execute(env);
	}
}
