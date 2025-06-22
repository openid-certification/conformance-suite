package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CreateVP1FinalVerifierIsoMdocRedirectSessionTranscriptEncrypted_UnitTest {

	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateVP1FinalVerifierIsoMdocRedirectSessionTranscriptEncrypted cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CreateVP1FinalVerifierIsoMdocRedirectSessionTranscriptEncrypted();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() throws Exception {
		// example from https://openid.net/specs/openid-4-verifiable-presentations-1_0-29.html#appendix-B.2.6.1
		env.putObjectFromJsonString("authorization_request_object", "claims.client_metadata.jwks", """
			{ "keys": [
			{
			  "kty": "EC",
			  "crv": "P-256",
			  "x": "DxiH5Q4Yx3UrukE2lWCErq8N8bqC9CHLLrAwLz5BmE0",
			  "y": "XtLM4-3h5o3HUH0MHVJV0kyq0iBlrBwlh8qEDMZ4-Pc",
			  "use": "enc",
			  "alg": "ECDH-ES",
			  "kid": "1"
			}
			]
			}
			""");
		env.putString("client_id", "x509_san_dns:example.com");
		env.putString("authorization_request_object", "claims.response_uri", "https://example.com/response"); // FIXME key name might be wrong
		env.putString("nonce", "exc7gBkxjx1rdc9udRrveKvSsJIq80avlXeLHhGwqtA");

		cond.execute(env);


		// this is the example in the spec:
		// 83f6f682714f70656e494434565048616e646f7665725820048bc053c00442af9b8eed494cefdd9d95240d254b046b11b68013722aad38ac
		// converted to base64 using https://cbor.zone
		String expected = "g/b2gnFPcGVuSUQ0VlBIYW5kb3ZlclggBIvAU8AEQq+bju1JTO/dnZUkDSVLBGsRtoATciqtOKw=";
		assertThat(env.getString("session_transcript")).isEqualTo(expected);
	}

	@Test
	public void testEvaluate_noErrorNoJwk() throws Exception {
		assertThrows(ConditionError.class, () -> {
			env.putString("client_id", "x509_san_dns:example.com");
			env.putString("authorization_request_object", "claims.response_uri", "https://example.com/response"); // FIXME key name might be wrong
			env.putString("nonce", "exc7gBkxjx1rdc9udRrveKvSsJIq80avlXeLHhGwqtA");

			cond.execute(env);
		});
	}

}
