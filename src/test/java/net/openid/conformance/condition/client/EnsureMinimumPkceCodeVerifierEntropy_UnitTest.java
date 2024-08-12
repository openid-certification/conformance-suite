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
public class EnsureMinimumPkceCodeVerifierEntropy_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureMinimumPkceCodeVerifierEntropy cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureMinimumPkceCodeVerifierEntropy();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_entropyGood() {
		// a real example from Filip's client that was previously causing a failure
		// assuming it's using the full alphanumeric character set it's 26+26+10 possible values so
		// ~5.95 bits per character, and 43 characters long, which should give space for
		// ~255 bits of entropy - but we're measuring approximately 189
		env.putString("token_endpoint_request", "body_form_params.code_verifier", "Nne6GqZXwX87rIt6IATnFw0lFl0qTmm1AEX69qXsmqA");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_entropyBad() {
		assertThrows(ConditionError.class, () -> {
			env.putString("token_endpoint_request", "body_form_params.code_verifier", "1111111111111111111111111111111111111111");

			cond.execute(env);
		});
	}

}
