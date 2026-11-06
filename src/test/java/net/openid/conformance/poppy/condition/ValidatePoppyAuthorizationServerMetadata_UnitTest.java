package net.openid.conformance.poppy.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidatePoppyAuthorizationServerMetadata_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidatePoppyAuthorizationServerMetadata cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidatePoppyAuthorizationServerMetadata();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putString("poppy_issuer", "https://auth.example.com");
		env.putString("poppy_domain", "example.com");
	}

	private ConditionError failWith(Consumer<JsonObject> mutate) {
		JsonObject server = PoppyTestData.json(PoppyTestData.AS_METADATA);
		mutate.accept(server);
		env.putObject("server", server);
		return assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void exampleMetadataPasses() {
		env.putObject("server", PoppyTestData.json(PoppyTestData.AS_METADATA));
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void issuerTrailingSlashFails() {
		failWith(s -> s.addProperty("issuer", "https://auth.example.com/"));
	}

	@Test
	public void protectedResourcesWithoutDomainFails() {
		failWith(s -> {
			JsonArray a = new JsonArray();
			a.add("https://example.co.uk");
			s.add("protected_resources", a);
		});
	}

	@Test
	public void protectedResourcesMissingFails() {
		failWith(s -> s.remove("protected_resources"));
	}

	@Test
	public void missingRefreshTokenGrantFails() {
		ConditionError e = failWith(s -> {
			JsonArray a = new JsonArray();
			a.add("authorization_code");
			s.add("grant_types_supported", a);
		});
		assertThat(e.getMessage()).contains("MUST include 'refresh_token'");
	}

	@Test
	public void missingAuthorizationCodeGrantFails() {
		ConditionError e = failWith(s -> {
			JsonArray a = new JsonArray();
			a.add("refresh_token");
			s.add("grant_types_supported", a);
		});
		assertThat(e.getMessage()).contains("does not offer Direct Sign-In");
	}

	@Test
	public void deviceCodeGrantWithoutDeviceEndpointFails() {
		ConditionError e = failWith(s -> s.remove("device_authorization_endpoint"));
		assertThat(e.getMessage()).contains("device_authorization_endpoint");
	}

	@Test
	public void noDeviceCodeGrantAndNoDeviceEndpointPasses() {
		JsonObject server = PoppyTestData.json(PoppyTestData.AS_METADATA);
		server.remove("device_authorization_endpoint");
		JsonArray a = new JsonArray();
		a.add("refresh_token");
		a.add("authorization_code");
		server.add("grant_types_supported", a);
		env.putObject("server", server);
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void missingTokenEndpointFails() {
		failWith(s -> s.remove("token_endpoint"));
	}

	@Test
	public void missingAuthorizationEndpointFails() {
		failWith(s -> s.remove("authorization_endpoint"));
	}

	@Test
	public void missingRevocationEndpointFails() {
		failWith(s -> s.remove("revocation_endpoint"));
	}

	@Test
	public void httpDeviceEndpointFails() {
		failWith(s -> s.addProperty("device_authorization_endpoint", "http://auth.example.com/oauth/device"));
	}

	@Test
	public void httpProtectedResourceEntryFails() {
		failWith(s -> s.getAsJsonArray("protected_resources").add("http://mcp.example.com/mcp"));
	}

	@Test
	public void httpJwksUriFails() {
		failWith(s -> s.addProperty("jwks_uri", "http://auth.example.com/jwks"));
	}

	@Test
	public void missingGrantTypesSupportedFails() {
		ConditionError e = failWith(s -> s.remove("grant_types_supported"));
		assertThat(e.getMessage()).contains("Required");
	}
}
