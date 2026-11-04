package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ExpectRequestObjectWithNbfOver10ClaimErrorPage_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ExpectRequestObjectWithNbfOver10ClaimErrorPage cond;

	@BeforeEach
	public void setUp() {
		cond = new ExpectRequestObjectWithNbfOver10ClaimErrorPage();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void storesThePlaceholderAndStatesTheTenMinuteLimit() {
		cond.execute(env);

		String placeholder = env.getString("request_object_unverifiable_error");
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(eventLog).log(anyString(), captor.capture());
		Map<String, Object> entry = captor.getValue();
		assertEquals(placeholder, entry.get("upload"));
		assertTrue(((String) entry.get("msg")).contains("10 minutes"), (String) entry.get("msg"));
	}
}
