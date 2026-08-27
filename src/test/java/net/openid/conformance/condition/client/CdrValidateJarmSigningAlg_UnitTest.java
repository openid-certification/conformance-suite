package net.openid.conformance.condition.client;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CdrValidateJarmSigningAlg_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CdrValidateJarmSigningAlg cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CdrValidateJarmSigningAlg();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void addJarmResponse(String alg) {
		env.putObject("jarm_response", JsonParser.parseString("{\"header\":{\"alg\":\"" + alg + "\"}}").getAsJsonObject());
	}

	@Test
	public void testEvaluate_ps256IsGood() {
		addJarmResponse("PS256");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_es256IsGood() {
		addJarmResponse("ES256");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_edDsaFails() {
		assertThrows(ConditionError.class, () -> {
			addJarmResponse("EdDSA");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_rs256Fails() {
		assertThrows(ConditionError.class, () -> {
			addJarmResponse("RS256");
			cond.execute(env);
		});
	}

}
