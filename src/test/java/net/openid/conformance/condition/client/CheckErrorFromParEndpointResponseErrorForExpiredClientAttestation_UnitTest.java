package net.openid.conformance.condition.client;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckErrorFromParEndpointResponseErrorForExpiredClientAttestation_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckErrorFromParEndpointResponseErrorForExpiredClientAttestation cond;

	private void putParResponse(String body) {
		env.putObject(CallPAREndpoint.RESPONSE_KEY,
			JsonParser.parseString("{\"status\":400,\"body_json\":" + body + "}").getAsJsonObject());
	}

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckErrorFromParEndpointResponseErrorForExpiredClientAttestation();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@ParameterizedTest
	@ValueSource(strings = {"invalid_request", "invalid_client", "invalid_client_attestation", "use_fresh_attestation"})
	public void testEvaluate_acceptedErrors(String error) {
		putParResponse("{\"error\":\"" + error + "\"}");
		cond.execute(env);
	}

	@ParameterizedTest
	@ValueSource(strings = {"invalid_grant", "use_attestation_challenge", "foo"})
	public void testEvaluate_unexpectedError(String error) {
		putParResponse("{\"error\":\"" + error + "\"}");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingErrorField() {
		putParResponse("{\"error_description\":\"expired\"}");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingResponse() {
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
