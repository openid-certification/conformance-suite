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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidatePoppyClientMetadataDocument_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidatePoppyClientMetadataDocument cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidatePoppyClientMetadataDocument();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void run(Consumer<JsonObject> mutate) {
		JsonObject doc = PoppyTestData.json(PoppyTestData.CLIENT_METADATA);
		mutate.accept(doc);
		env.putObject("client", doc);
		env.putString("poppy_incoming_client_id", "https://agent.example/agent.json");
		cond.execute(env);
	}

	private static void redirect(JsonObject doc, String uri) {
		JsonArray uris = new JsonArray();
		uris.add(uri);
		doc.add("redirect_uris", uris);
	}

	@Test
	public void specExamplePasses() {
		assertDoesNotThrow(() -> run(d -> { }));
	}

	@Test
	public void clientIdMismatchFails() {
		assertThrows(ConditionError.class, () -> run(d -> d.addProperty("client_id", "https://agent.example/other.json")));
	}

	@Test
	public void missingClientIdFails() {
		assertThrows(ConditionError.class, () -> run(d -> d.remove("client_id")));
	}

	@Test
	public void jwksUriOtherHostFails() {
		assertThrows(ConditionError.class, () -> run(d -> d.addProperty("jwks_uri", "https://evil.example/jwks.json")));
	}

	@Test
	public void jwksUriHttpFails() {
		assertThrows(ConditionError.class, () -> run(d -> d.addProperty("jwks_uri", "http://agent.example/jwks.json")));
	}

	@Test
	public void missingJwksUriFails() {
		assertThrows(ConditionError.class, () -> run(d -> d.remove("jwks_uri")));
	}

	@Test
	public void redirectUriOtherHostFails() {
		assertThrows(ConditionError.class, () -> run(d -> redirect(d, "https://evil.example/cb")));
	}

	@Test
	public void redirectUrisMissingFails() {
		assertThrows(ConditionError.class, () -> run(d -> d.remove("redirect_uris")));
	}

	@Test
	public void redirectUrisEmptyFails() {
		assertThrows(ConditionError.class, () -> run(d -> d.add("redirect_uris", new JsonArray())));
	}

	@Test
	public void loopbackIpv4Passes() {
		assertDoesNotThrow(() -> run(d -> redirect(d, "http://127.0.0.1:49152/cb")));
	}

	@Test
	public void loopbackIpv6Passes() {
		assertDoesNotThrow(() -> run(d -> redirect(d, "http://[::1]:49152/cb")));
	}

	@Test
	public void localhostFails() {
		assertThrows(ConditionError.class, () -> run(d -> redirect(d, "http://localhost/cb")));
	}

	@Test
	public void authMethodOtherFails() {
		assertThrows(ConditionError.class, () -> run(d -> d.addProperty("token_endpoint_auth_method", "client_secret_basic")));
	}

	@Test
	public void authMethodAbsentPasses() {
		assertDoesNotThrow(() -> run(d -> d.remove("token_endpoint_auth_method")));
	}
}
