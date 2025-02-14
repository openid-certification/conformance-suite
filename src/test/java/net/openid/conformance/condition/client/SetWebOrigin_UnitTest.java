package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class SetWebOrigin_UnitTest {

	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private SetWebOrigin cond;


	@BeforeEach
	public void setUp() throws Exception {
		cond = new SetWebOrigin();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_valuePresentLocalhost() {

		env.putString("base_url", "https://localhost.emobix.co.uk:8443/test/a/oidf-vp-test-verifier");

		cond.execute(env);

		assertThat(env.getString("origin")).isEqualTo("https://localhost.emobix.co.uk:8443");

	}

	@Test
	public void testEvaluate_valuePresentDemo() {

		env.putString("base_url", "https://demo.certification.openid.net/test/a/oidf-vp-test-wallet");

		cond.execute(env);

		assertThat(env.getString("origin")).isEqualTo("https://demo.certification.openid.net");

	}

}
