package net.openid.conformance.condition.common;

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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class EnsureIncomingTls13_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureIncomingTls13 cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureIncomingTls13();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_successTls13() {
		JsonObject req = JsonParser.parseString("{\"headers\": {\"x-ssl-protocol\": \"TLSv1.3\"}}").getAsJsonObject();
		env.putObject("client_request", req);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("client_request", "headers.x-ssl-protocol");
	}

	@Test
	public void testEvaluate_failTls12() {
		assertThrows(ConditionError.class, () -> {
			JsonObject req = JsonParser.parseString("{\"headers\": {\"x-ssl-protocol\": \"TLSv1.2\"}}").getAsJsonObject();
			env.putObject("client_request", req);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_failTls11() {
		assertThrows(ConditionError.class, () -> {
			JsonObject req = JsonParser.parseString("{\"headers\": {\"x-ssl-protocol\": \"TLSv1.1\"}}").getAsJsonObject();
			env.putObject("client_request", req);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingProtocol() {
		assertThrows(ConditionError.class, () -> {
			JsonObject req = JsonParser.parseString("{\"headers\": {}}").getAsJsonObject();
			env.putObject("client_request", req);
			cond.execute(env);
		});
	}

}
