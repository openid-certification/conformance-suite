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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class SetClient2IdToCurrentClientId_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private SetClient2IdToCurrentClientId cond;

	@BeforeEach
	public void setUp() {
		cond = new SetClient2IdToCurrentClientId();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_success() {
		env.putString("client_id", "x509_san_dns:wallet.example.org");

		cond.execute(env);

		assertThat(env.getString("client2_id")).isEqualTo("x509_san_dns:wallet.example.org");
	}

	@Test
	public void testEvaluate_missingClientId() {
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
