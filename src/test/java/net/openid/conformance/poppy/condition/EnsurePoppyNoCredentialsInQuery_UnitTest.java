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
public class EnsurePoppyNoCredentialsInQuery_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsurePoppyNoCredentialsInQuery cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsurePoppyNoCredentialsInQuery();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void run(String query) {
		env.putObject("incoming_request", JsonParser.parseString("{\"query_string_params\":" + query + "}").getAsJsonObject());
		cond.execute(env);
	}

	@Test
	public void emptyQueryPasses() {
		assertDoesNotThrow(() -> run("{}"));
	}

	@Test
	public void otherParametersPass() {
		assertDoesNotThrow(() -> run("{\"cursor\":\"1\"}"));
	}

	@Test
	public void accessTokenFails() {
		assertThrows(ConditionError.class, () -> run("{\"access_token\":\"x\"}"));
	}

	@Test
	public void parameterNamesAreCaseSensitive() {
		assertDoesNotThrow(() -> run("{\"Access_Token\":\"x\"}"));
		assertThrows(ConditionError.class, () -> run("{\"access_token\":\"x\"}"));
	}

	@Test
	public void sessionFails() {
		assertThrows(ConditionError.class, () -> run("{\"poppy_session\":\"x\"}"));
	}
}
