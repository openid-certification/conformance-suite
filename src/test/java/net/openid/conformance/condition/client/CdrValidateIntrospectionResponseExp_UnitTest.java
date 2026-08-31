package net.openid.conformance.condition.client;

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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CdrValidateIntrospectionResponseExp_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CdrValidateIntrospectionResponseExp cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CdrValidateIntrospectionResponseExp();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void addResponseWithExp(long exp) {
		env.putObject(CallTokenIntrospectionEndpoint.RESPONSE_KEY,
			JsonParser.parseString("{\"body_json\":{\"active\":true,\"exp\":" + exp + "}}").getAsJsonObject());
	}

	@Test
	public void testEvaluate_matchesSharingDuration() {
		addResponseWithExp(Instant.now().getEpochSecond()
			+ AddCdrSharingDurationClaimToAuthorizationEndpointRequest.SHARING_DURATION_SECONDS);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_expiresTooSoon() {
		assertThrows(ConditionError.class, () -> {
			// e.g. a refresh token issued with a fixed 28 day lifetime instead of the sharing duration
			addResponseWithExp(Instant.now().getEpochSecond() + 28 * 24 * 3600);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_expiresTooLate() {
		assertThrows(ConditionError.class, () -> {
			addResponseWithExp(Instant.now().getEpochSecond() + 365 * 24 * 3600);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingExp() {
		assertThrows(ConditionError.class, () -> {
			env.putObject(CallTokenIntrospectionEndpoint.RESPONSE_KEY,
				JsonParser.parseString("{\"body_json\":{\"active\":true}}").getAsJsonObject());
			cond.execute(env);
		});
	}

}
