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
class VCIValidateCredentialIssuerUri_UnitTest {

	private VCIValidateCredentialIssuerUri cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new VCIValidateCredentialIssuerUri();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	void acceptsMatchingHttpsIssuer() {
		putConfig("https://issuer.example.com");
		putMetadata("""
			{"credential_issuer": "https://issuer.example.com"}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void acceptsMatchingHttpsIssuerWithPath() {
		putConfig("https://issuer.example.com/tenant/foo");
		putMetadata("""
			{"credential_issuer": "https://issuer.example.com/tenant/foo"}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsCredentialIssuerNotMatchingConfiguredUrl() {
		putConfig("https://issuerA.example.com");
		putMetadata("""
			{"credential_issuer": "https://issuerB.example.com"}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsCredentialIssuerWithTrailingSlashMismatch() {
		// OID4VCI 1.0 Final §12.2.3 requires byte-identical equality — a trailing
		// slash difference is a real mismatch, not a normalization concern.
		putConfig("https://issuer.example.com");
		putMetadata("""
			{"credential_issuer": "https://issuer.example.com/"}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsHttpScheme() {
		putConfig("http://issuer.example.com");
		putMetadata("""
			{"credential_issuer": "http://issuer.example.com"}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void acceptsUppercaseHttpsScheme() {
		// RFC 3986 §3.1 says scheme names are case-insensitive.
		putConfig("HTTPS://issuer.example.com");
		putMetadata("""
			{"credential_issuer": "HTTPS://issuer.example.com"}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsFragment() {
		putConfig("https://issuer.example.com/#frag");
		putMetadata("""
			{"credential_issuer": "https://issuer.example.com/#frag"}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsQuery() {
		putConfig("https://issuer.example.com/?foo=bar");
		putMetadata("""
			{"credential_issuer": "https://issuer.example.com/?foo=bar"}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void acceptsWhenConfigUrlAbsent() {
		// If the test config doesn't have vci.credential_issuer_url set the equality
		// check is skipped — only URI syntax is enforced.
		putMetadata("""
			{"credential_issuer": "https://issuer.example.com"}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void acceptsWhenConfigUrlBlank() {
		// Whitespace-only configured value is treated as 'no value set'.
		putConfig("   ");
		putMetadata("""
			{"credential_issuer": "https://issuer.example.com"}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsMissingCredentialIssuer() {
		putMetadata("""
			{"credential_endpoint": "https://issuer.example.com/credential"}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsNonStringCredentialIssuer() {
		putMetadata("""
			{"credential_issuer": 42}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsMalformedUri() {
		putMetadata("""
			{"credential_issuer": "not a uri >>>"}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsAuthorityLessUri() {
		// 'https:/path' parses with scheme=https but no authority/host; a Credential Issuer
		// Identifier per OID4VCI 1.0 Final §12.2.1 must be a full URL with a host component.
		putMetadata("""
			{"credential_issuer": "https:/path"}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsUserInfo() {
		putConfig("https://user:pass@issuer.example.com");
		putMetadata("""
			{"credential_issuer": "https://user:pass@issuer.example.com"}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsOutOfRangePort() {
		putConfig("https://issuer.example.com:99999");
		putMetadata("""
			{"credential_issuer": "https://issuer.example.com:99999"}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putMetadata(String json) {
		JsonObject metadata = JsonParser.parseString(json).getAsJsonObject();
		JsonObject vci = new JsonObject();
		vci.add("credential_issuer_metadata", metadata);
		env.putObject("vci", vci);
	}

	private void putConfig(String issuer) {
		JsonObject vci = new JsonObject();
		vci.addProperty("credential_issuer_url", issuer);
		JsonObject config = new JsonObject();
		config.add("vci", vci);
		env.putObject("config", config);
	}
}
