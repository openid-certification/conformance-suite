package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class VCIValidateAuthorizationServersAreHttps_UnitTest extends AbstractVciUnitTest {

	private VCIValidateAuthorizationServersAreHttps cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new VCIValidateAuthorizationServersAreHttps();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	void noErrorWhenAuthorizationServersAbsent() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential"
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void acceptsSingleHttpsEntry() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "authorization_servers": ["https://as.example.com"]
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void acceptsMultipleHttpsEntries() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "authorization_servers": ["https://as1.example.com", "https://as2.example.com/path"]
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsHttpScheme() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "authorization_servers": ["http://as.example.com"]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsFragment() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "authorization_servers": ["https://as.example.com/#frag"]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsQuery() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "authorization_servers": ["https://as.example.com/?foo=bar"]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsNonStringEntry() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "authorization_servers": [123]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void acceptsUppercaseHttpsScheme() {
		// RFC 3986 §3.1 defines URI scheme names as case-insensitive.
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "authorization_servers": ["HTTPS://as.example.com"]
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsEmptyArray() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "authorization_servers": []
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsUserInfo() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "authorization_servers": ["https://user:pass@as.example.com/"]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsOutOfRangePort() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "authorization_servers": ["https://as.example.com:99999"]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsAuthorityLessUri() {
		// 'https:/metadata' parses with scheme=https, path=/metadata, no authority.
		// Issuer identifiers per RFC 8414 must be full URLs with a host component.
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "authorization_servers": ["https:/metadata"]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsNonArray() {
		putCredentialIssuerMetadata("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "credential_endpoint": "https://issuer.example.com/credential",
			  "authorization_servers": "https://as.example.com"
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putCredentialIssuerMetadata(String json) {
		JsonObject metadata = JsonParser.parseString(json).getAsJsonObject();
		JsonObject vci = new JsonObject();
		vci.add("credential_issuer_metadata", metadata);
		env.putObject("vci", vci);
	}
}
