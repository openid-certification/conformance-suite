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
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class SelectPoppyIssuerFromProtectedResourceMetadata_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private SelectPoppyIssuerFromProtectedResourceMetadata cond;

	@BeforeEach
	public void setUp() {
		cond = new SelectPoppyIssuerFromProtectedResourceMetadata();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putString("poppy_domain", "example.com");
	}

	private void setup(String metadata, String authIssuer) {
		env.putObject("poppy_protected_resource_metadata", PoppyTestData.json(metadata));
		String auth = authIssuer == null ? "" : ",\"auth\":{\"issuer\":\"" + authIssuer + "\"}";
		env.putObject("poppy_json", PoppyTestData.json("{\"protocol_version\":\"0.4\"" + auth + "}"));
	}

	private static String md(String resource, String servers) {
		return "{\"resource\":\"" + resource + "\",\"authorization_servers\":" + servers + "}";
	}

	@Test
	public void singleServerSelected() {
		setup(md("https://example.com", "[\"https://auth.example.com\"]"), null);
		cond.execute(env);
		assertThat(env.getString("poppy_issuer")).isEqualTo("https://auth.example.com");
	}

	@Test
	public void trailingSlashResourceFails() {
		setup(md("https://example.com/", "[\"https://auth.example.com\"]"), null);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void httpResourceFails() {
		setup(md("http://example.com", "[\"https://auth.example.com\"]"), null);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void httpAuthorizationServerFails() {
		setup(md("https://example.com", "[\"http://auth.example.com\"]"), null);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void httpJwksUriFails() {
		setup("{\"resource\":\"https://example.com\",\"authorization_servers\":[\"https://auth.example.com\"],\"jwks_uri\":\"http://example.com/jwks\"}", null);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void httpEndpointKeyFails() {
		setup("{\"resource\":\"https://example.com\",\"authorization_servers\":[\"https://auth.example.com\"],\"foo_endpoint\":\"http://example.com/x\"}", null);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void twoServersWithoutAuthIssuerFails() {
		setup(md("https://example.com", "[\"https://a.example.com\",\"https://b.example.com\"]"), null);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(e.getMessage()).contains("auth.issuer");
	}

	@Test
	public void twoServersWithAuthIssuerSelectsIt() {
		setup(md("https://example.com", "[\"https://a.example.com\",\"https://b.example.com\"]"), "https://b.example.com");
		cond.execute(env);
		assertThat(env.getString("poppy_issuer")).isEqualTo("https://b.example.com");
	}

	@Test
	public void authIssuerNotInListFails() {
		setup(md("https://example.com", "[\"https://a.example.com\"]"), "https://c.example.com");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void emptyListFails() {
		setup(md("https://example.com", "[]"), null);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(e.getMessage()).contains("no sign-in");
	}

	@Test
	public void uppercaseHostResourceAccepted() {
		setup(md("https://Example.com", "[\"https://auth.example.com\"]"), null);
		cond.execute(env);
		assertThat(env.getString("poppy_issuer")).isEqualTo("https://auth.example.com");
	}
}
