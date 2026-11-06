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
public class EnsurePoppyClientAssertionAlgIsAdvertised_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsurePoppyClientAssertionAlgIsAdvertised cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsurePoppyClientAssertionAlgIsAdvertised();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void run(String alg) {
		env.putObject("client_assertion", JsonParser.parseString("{\"header\":{\"alg\":\"" + alg + "\"}}").getAsJsonObject());
		env.putObject("server", JsonParser.parseString("{\"token_endpoint_auth_signing_alg_values_supported\":[\"ES256\",\"PS384\"]}").getAsJsonObject());
		cond.execute(env);
	}

	@Test
	public void advertisedAlgsPass() {
		assertDoesNotThrow(() -> run("ES256"));
		assertDoesNotThrow(() -> run("PS384"));
	}

	@Test
	public void unadvertisedAlgFails() {
		assertThrows(ConditionError.class, () -> run("RS256"));
	}

	@Test
	public void noneFails() {
		assertThrows(ConditionError.class, () -> run("none"));
	}
}
