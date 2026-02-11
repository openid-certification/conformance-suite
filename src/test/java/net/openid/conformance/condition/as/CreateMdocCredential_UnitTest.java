package net.openid.conformance.condition.as;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.ParseCredentialAsMdoc;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class CreateMdocCredential_UnitTest {

	private final Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateMdocCredential cond;

	@BeforeEach
	public void setUp() {
		cond = new CreateMdocCredential();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_createsParsableCredential() {
		env.putString("session_transcript", "g/b2gnZPcGVuSUQ0VlBEQ0FQSUhhbmRvdmVyWCBd0cMpz6ie3V5hrfH0TMRNv/K/U1jcr0o2rN+i0gMNWA==");

		cond.execute(env);

		String credential = env.getString("credential");
		assertThat(credential).isNotBlank();

		ParseCredentialAsMdoc parseCond = new ParseCredentialAsMdoc();
		parseCond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		parseCond.execute(env);
	}
}
