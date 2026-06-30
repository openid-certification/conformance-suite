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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckParEndpointHttpStatusIs400Allowing401ForInvalidClientError_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckParEndpointHttpStatusIs400Allowing401ForInvalidClientError cond;

	private void putParResponse(int status, String errorValue) {
		env.putObject(CallPAREndpoint.RESPONSE_KEY,
			JsonParser.parseString("{\"status\":" + status + ",\"body_json\":{\"error\":\"" + errorValue + "\"}}").getAsJsonObject());
	}

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckParEndpointHttpStatusIs400Allowing401ForInvalidClientError();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_caseInvalidRequest() {
		putParResponse(400, "invalid_request");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseInvalidClient400() {
		putParResponse(400, "invalid_client");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseInvalidClient401() {
		putParResponse(401, "invalid_client");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseInvalidRequestObject400() {
		putParResponse(400, "invalid_request_object");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseBadHttpStatusInvalidRequest() {
		assertThrows(ConditionError.class, () -> {
			putParResponse(401, "invalid_request");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_caseBadHttpStatus500InvalidRequest() {
		assertThrows(ConditionError.class, () -> {
			putParResponse(500, "invalid_request");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_caseBadHttpStatusInvalidClient() {
		assertThrows(ConditionError.class, () -> {
			putParResponse(402, "invalid_client");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_caseBadHttpStatus200InvalidClient() {
		assertThrows(ConditionError.class, () -> {
			putParResponse(200, "invalid_client");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_caseBadHttpStatusInvalidRequestObject() {
		assertThrows(ConditionError.class, () -> {
			putParResponse(401, "invalid_request_object");
			cond.execute(env);
		});
	}
}
