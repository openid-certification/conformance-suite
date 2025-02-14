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
public class CreateVerifierIsoMdocDCAPISessionTranscript_UnitTest {

	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateVerifierIsoMdocDCAPISessionTranscript cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CreateVerifierIsoMdocDCAPISessionTranscript();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() throws Exception {
		env.putString("client_id", "localhost.emobix.co.uk");
		env.putString("origin", "https://localhost.emobix.co.uk:8443");
		env.putString("nonce", "8sigA7tG9GnYfWeRfrAG5PMpHOif-._~");

		cond.execute(env);

		String expected = "g/b2gnZPcGVuSUQ0VlBEQ0FQSUhhbmRvdmVyWCBd0cMpz6ie3V5hrfH0TMRNv/K/U1jcr0o2rN+i0gMNWA==";
		assertThat(env.getString("session_transcript")).isEqualTo(expected);
	}

}
