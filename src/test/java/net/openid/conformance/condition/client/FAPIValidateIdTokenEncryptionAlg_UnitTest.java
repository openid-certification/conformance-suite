package net.openid.conformance.condition.client;

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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class FAPIValidateIdTokenEncryptionAlg_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private FAPIValidateIdTokenEncryptionAlg cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new FAPIValidateIdTokenEncryptionAlg();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {
		env.putString("id_token", "jwe_header.alg", "A256GCM");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_rsa15() {
		assertThrows(ConditionError.class, () -> {
			env.putString("id_token", "jwe_header.alg", "RSA1_5");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_symmetric() {
		assertThrows(ConditionError.class, () -> {
			env.putString("id_token", "jwe_header.alg", "A128GCMKW");
			cond.execute(env);
		});
	}

}
