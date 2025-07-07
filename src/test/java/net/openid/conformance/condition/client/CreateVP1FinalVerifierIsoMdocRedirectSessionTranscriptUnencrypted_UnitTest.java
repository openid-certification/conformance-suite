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
public class CreateVP1FinalVerifierIsoMdocRedirectSessionTranscriptUnencrypted_UnitTest {

	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateVP1FinalVerifierIsoMdocRedirectSessionTranscriptUnencrypted cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CreateVP1FinalVerifierIsoMdocRedirectSessionTranscriptUnencrypted();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noErrorNoJwk() throws Exception {
		env.putString("client_id", "x509_san_dns:example.com");
		env.putString("authorization_request_object", "claims.response_uri", "https://example.com/response"); // FIXME key name might be wrong
		env.putString("nonce", "exc7gBkxjx1rdc9udRrveKvSsJIq80avlXeLHhGwqtA");

		cond.execute(env);
		// we don't have a known expected output for this so this is just what the code generates currently
		String expected = "g/b2gnFPcGVuSUQ0VlBIYW5kb3ZlclggxnK1Si+s+zKEllxb6DiozR+W99427eNHCx1d4sV9dxk=";
		assertThat(env.getString("session_transcript")).isEqualTo(expected);
	}

}
