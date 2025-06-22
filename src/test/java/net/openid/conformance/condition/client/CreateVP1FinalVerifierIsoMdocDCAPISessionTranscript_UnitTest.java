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
public class CreateVP1FinalVerifierIsoMdocDCAPISessionTranscript_UnitTest {

	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateVP1FinalVerifierIsoMdocDCAPISessionTranscript cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CreateVP1FinalVerifierIsoMdocDCAPISessionTranscript();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() throws Exception {
		// example from https://openid.net/specs/openid-4-verifiable-presentations-1_0-29.html#appendix-B.2.6.2
		env.putObjectFromJsonString("decryption_jwk", """
			{
			  "kty": "EC",
			  "crv": "P-256",
			  "x": "DxiH5Q4Yx3UrukE2lWCErq8N8bqC9CHLLrAwLz5BmE0",
			  "y": "XtLM4-3h5o3HUH0MHVJV0kyq0iBlrBwlh8qEDMZ4-Pc",
			  "use": "enc",
			  "alg": "ECDH-ES",
			  "kid": "1"
			}
			""");
		env.putString("origin", "https://example.com");
		env.putString("nonce", "exc7gBkxjx1rdc9udRrveKvSsJIq80avlXeLHhGwqtA");

		cond.execute(env);


		// this is the example in the spec:
		// 83f6f682764f70656e4944345650444341504948616e646f7665725820fbece366f4212f9762c74cfdbf83b8c69e371d5d68cea09cb4c48ca6daab761a
		// converted to base64 using https://cbor.zone
		String expected = "g/b2gnZPcGVuSUQ0VlBEQ0FQSUhhbmRvdmVyWCD77ONm9CEvl2LHTP2/g7jGnjcdXWjOoJy0xIym2qt2Gg==";
		assertThat(env.getString("session_transcript")).isEqualTo(expected);
	}

	@Test
	public void testEvaluate_noErrorNoJwk() throws Exception {
		env.putString("origin", "https://example.com");
		env.putString("nonce", "exc7gBkxjx1rdc9udRrveKvSsJIq80avlXeLHhGwqtA");

		cond.execute(env);
		// we don't have a known expected output for this so this is just what the code generates currently
		String expected = "g/b2gnZPcGVuSUQ0VlBEQ0FQSUhhbmRvdmVyWCA1Y7Yp4ovwpzbGum+qy2CpzTB05RQWoEVlAx2GkGOMQw==";
		assertThat(env.getString("session_transcript")).isEqualTo(expected);
	}

}
