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
public class EnsurePoppyResourceResponseStatusIs2xx_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsurePoppyResourceResponseStatusIs2xx cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsurePoppyResourceResponseStatusIs2xx();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void run(String json) {
		env.putObject("resource_endpoint_response_full", JsonParser.parseString(json).getAsJsonObject());
		cond.execute(env);
	}

	@Test
	public void twoXxPasses() {
		assertDoesNotThrow(() -> run("{\"status\":200}"));
		assertDoesNotThrow(() -> run("{\"status\":204}"));
	}

	private String failureLog(String json) {
		TestInstanceEventLog log = org.mockito.Mockito.mock(TestInstanceEventLog.class);
		cond.setProperties("UNIT-TEST", log, ConditionResult.INFO);
		assertThrows(ConditionError.class, () -> run(json));
		org.mockito.ArgumentCaptor<java.util.Map<String, Object>> entries = org.mockito.ArgumentCaptor.captor();
		org.mockito.Mockito.verify(log, org.mockito.Mockito.atLeastOnce()).log(org.mockito.ArgumentMatchers.anyString(), entries.capture());
		return entries.getAllValues().toString();
	}

	@Test
	public void unauthorizedFailsWithHeader() {
		assertTrue(failureLog("{\"status\":401,\"headers\":{\"www-authenticate\":\"Bearer error=\\\"invalid_token\\\"\"}}").contains("invalid_token"));
	}

	@Test
	public void badRequestFailsWithBody() {
		assertTrue(failureLog("{\"status\":400,\"body\":\"{\\\"error\\\":\\\"invalid_session\\\"}\"}").contains("invalid_session"));
	}
}
