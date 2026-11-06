package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class EnsurePoppyRedirectUriMatchesClientMetadata_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsurePoppyRedirectUriMatchesClientMetadata cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsurePoppyRedirectUriMatchesClientMetadata();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void run(String registered, String requested) {
		env.putObject("client", JsonParser.parseString("{\"redirect_uris\":[\"" + registered + "\"]}").getAsJsonObject());
		JsonObject req = new JsonObject();
		if (requested != null) {
			req.addProperty("redirect_uri", requested);
		}
		env.putObject("effective_authorization_endpoint_request", req);
		cond.execute(env);
	}

	@Test
	public void exactMatchPasses() {
		run("https://agent.example/cb", "https://agent.example/cb");
		assertEquals("https://agent.example/cb", env.getString("authorization_endpoint_request_redirect_uri"));
	}

	@Test
	public void differentPathFails() {
		assertThrows(ConditionError.class, () -> run("https://agent.example/cb", "https://agent.example/other"));
	}

	@Test
	public void loopbackPortMayDiffer() {
		run("http://127.0.0.1:49152/cb", "http://127.0.0.1:50000/cb");
		assertEquals("http://127.0.0.1:50000/cb", env.getString("authorization_endpoint_request_redirect_uri"));
	}

	@Test
	public void loopbackPathMustMatch() {
		assertThrows(ConditionError.class, () -> run("http://127.0.0.1:49152/cb", "http://127.0.0.1:50000/x"));
	}

	@Test
	public void portExceptionIsLoopbackOnly() {
		assertThrows(ConditionError.class, () -> run("https://agent.example/cb", "https://agent.example:9000/cb"));
	}

	@Test
	public void absentRedirectUriFails() {
		assertThrows(ConditionError.class, () -> run("https://agent.example/cb", null));
	}
}
