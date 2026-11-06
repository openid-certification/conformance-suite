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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class GeneratePoppyCompanyDocuments_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private GeneratePoppyCompanyDocuments cond;

	@BeforeEach
	public void setUp() {
		cond = new GeneratePoppyCompanyDocuments();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void setUpEnv(String baseUrl, String domain) {
		env.putString("base_url", baseUrl);
		env.putObject("config", PoppyTestData.json("{\"poppy\": {\"company_domain\": \"" + domain + "\"}}"));
	}

	@Test
	public void generatesDocuments() {
		setUpEnv("https://h:8443/test/a/root-well-known", "h");
		cond.execute(env);
		String issuer = "https://h:8443/test/a/root-well-known";
		assertThat(env.getString("issuer")).isEqualTo(issuer);
		assertThat(env.getString("poppy_domain")).isEqualTo("h");

		assertThat(env.getObject("poppy_json")).isEqualTo(PoppyTestData.json("""
			{
			  "protocol_version": "0.4",
			  "organization": {"name": "OpenID Foundation conformance suite", "domain": "h"},
			  "auth": {
			    "scopes": {"orders:read": "See your orders"},
			    "scope_hints": {"orders:read": {"changes": "none", "personal_data": true}}
			  },
			  "apis": [{"type": "openapi", "url": "%s/openapi.json", "description": "Conformance suite test API"}]
			}
			""".formatted(issuer)));
		assertThat(env.getObject("poppy_protected_resource_metadata")).isEqualTo(PoppyTestData.json(
			"{\"resource\": \"https://h\", \"authorization_servers\": [\"" + issuer + "\"]}"));
		assertThat(env.getObject("server")).isEqualTo(PoppyTestData.json("""
			{
			  "issuer": "%1$s",
			  "authorization_endpoint": "%1$s/authorize",
			  "token_endpoint": "%1$s/token",
			  "revocation_endpoint": "%1$s/revoke",
			  "grant_types_supported": ["authorization_code", "refresh_token"],
			  "scopes_supported": ["orders:read"],
			  "protected_resources": ["https://h"],
			  "token_endpoint_auth_methods_supported": ["private_key_jwt"],
			  "token_endpoint_auth_signing_alg_values_supported": ["ES256", "PS384"],
			  "code_challenge_methods_supported": ["S256"],
			  "response_types_supported": ["code"]
			}
			""".formatted(issuer)));
	}

	@Test
	public void trailingSlashStripped() {
		setUpEnv("https://h:8443/test/a/root-well-known/", "h");
		cond.execute(env);
		assertThat(env.getString("issuer")).isEqualTo("https://h:8443/test/a/root-well-known");
	}

	@Test
	public void domainIsLowercased() {
		setUpEnv("https://h:8443/x", "H.Example");
		cond.execute(env);
		assertThat(env.getString("poppy_domain")).isEqualTo("h.example");
	}

	@Test
	public void domainWithPortFails() {
		setUpEnv("https://h:8443/x", "h:8443");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(e.getMessage()).contains("test configuration");
	}

	@Test
	public void domainWithSchemeFails() {
		setUpEnv("https://h:8443/x", "https://h");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(e.getMessage()).contains("test configuration");
	}

	@Test
	public void missingDomainFails() {
		env.putString("base_url", "https://h/x");
		env.putObject("config", new com.google.gson.JsonObject());
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(e.getMessage()).contains("test configuration");
	}
}
