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
public class CreateVerifierIsoMdlAnnexBSessionTranscript_UnitTest {

	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateVerifierIsoMdlAnnexBSessionTranscript cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CreateVerifierIsoMdlAnnexBSessionTranscript();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() throws Exception {

		env.putString("config", "client.client_id", "localhost.emobix.co.uk");
		env.putString("response_uri", "https://localhost.emobix.co.uk:8443/test/a/oidf-vp-test-verifier/responseuri");
		env.putString("nonce", "8sigA7tG9GnYfWeRfrAG5PMpHOif-._~");
		env.putString("mdoc_generated_nonce", "Vin3Z2f2d9oAAaq_bOMnYQ");

		cond.execute(env);

		String expected = "g/b2g1ggOEbrv0Z/TUtIFyLM5TDoNisYjlGK+Qx5dhFacE7xFz9YIPu4g9QyWrWNEI1T15XOjGVh9PezAUTVhIBTtU/aY4q0eCA4c2lnQTd0RzlHbllmV2VSZnJBRzVQTXBIT2lmLS5ffg==";
		assertThat(env.getString("session_transcript")).isEqualTo(expected);
	}

}
