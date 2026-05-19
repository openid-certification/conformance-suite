package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
public class CheckForUnexpectedParametersInAuthorizationServerMetadata_UnitTest extends AbstractVciUnitTest {

	private CheckForUnexpectedParametersInAuthorizationServerMetadata cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckForUnexpectedParametersInAuthorizationServerMetadata();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	public void testEvaluate_noWarningWhenNoUnknownProperties() {
		String json = """
			{
			  "issuer": "https://auth.example.com",
			  "authorization_endpoint": "https://auth.example.com/authorize",
			  "token_endpoint": "https://auth.example.com/token",
			  "response_types_supported": ["code"]
			}
			""";
		putAuthServerMetadata(json);
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_unknownTopLevelProperty() {
		String json = """
			{
			  "issuer": "https://auth.example.com",
			  "unexpected_field": "boom"
			}
			""";
		putAuthServerMetadata(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertUnknownPropertyAtPath(data, "$.unexpected_field");
	}

	@Test
	public void testEvaluate_wellKnownExtensionsDoNotWarn() {
		String json = """
			{
			  "issuer": "https://auth.example.com",
			  "authorization_endpoint": "https://auth.example.com/authorize",
			  "token_endpoint": "https://auth.example.com/token",
			  "dpop_signing_alg_values_supported": ["ES256"],
			  "pushed_authorization_request_endpoint": "https://auth.example.com/par",
			  "require_pushed_authorization_requests": true,
			  "authorization_details_types_supported": ["openid_credential"],
			  "userinfo_endpoint": "https://auth.example.com/userinfo",
			  "subject_types_supported": ["public"],
			  "id_token_signing_alg_values_supported": ["ES256"],
			  "mtls_endpoint_aliases": {},
			  "tls_client_certificate_bound_access_tokens": true,
			  "signed_metadata": "eyJ..."
			}
			""";
		putAuthServerMetadata(json);
		assertDoesNotThrow(() -> cond.execute(env));
	}

	private void putAuthServerMetadata(String json) {
		JsonObject metadata = JsonParser.parseString(json).getAsJsonObject();
		env.putObject("server", metadata);
	}
}
