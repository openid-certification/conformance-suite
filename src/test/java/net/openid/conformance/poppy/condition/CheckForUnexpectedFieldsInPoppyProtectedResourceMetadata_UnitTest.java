package net.openid.conformance.poppy.condition;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckForUnexpectedFieldsInPoppyProtectedResourceMetadata_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckForUnexpectedFieldsInPoppyProtectedResourceMetadata cond;

	@BeforeEach
	public void setUp() {
		cond = new CheckForUnexpectedFieldsInPoppyProtectedResourceMetadata();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void allRfc9728FieldsPass() {
		env.putObject("poppy_protected_resource_metadata", PoppyTestData.json("""
			{"resource": "https://example.com", "authorization_servers": ["https://auth.example.com"],
			 "jwks_uri": "https://example.com/jwks", "scopes_supported": [], "bearer_methods_supported": ["header"],
			 "resource_signing_alg_values_supported": ["ES256"], "resource_name": "x", "resource_documentation": "https://example.com/d",
			 "resource_policy_uri": "https://example.com/p", "resource_tos_uri": "https://example.com/t",
			 "tls_client_certificate_bound_access_tokens": false, "authorization_details_types_supported": [],
			 "dpop_signing_alg_values_supported": ["ES256"], "dpop_bound_access_tokens_required": false, "signed_metadata": "a.b.c"}
			"""));
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void unknownFieldFails() {
		env.putObject("poppy_protected_resource_metadata", PoppyTestData.json("{\"resource\": \"https://example.com\", \"foo\": 1}"));
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
