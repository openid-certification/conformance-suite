package net.openid.conformance.condition.client;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_caseInvalidRequest() {
		env.putInteger("token_endpoint_response_http_status", 400);
		env.putObject("token_endpoint_response", JsonParser.parseString("{\"error\":\"invalid_request\"}").getAsJsonObject());

		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseInvalidClient() {
		env.putInteger("token_endpoint_response_http_status", 401);
		env.putObject("token_endpoint_response", JsonParser.parseString("{\"error\":\"invalid_client\"}").getAsJsonObject());

		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseInvalidClient400() {
		env.putInteger("token_endpoint_response_http_status", 400);
		env.putObject("token_endpoint_response", JsonParser.parseString("{\"error\":\"invalid_client\"}").getAsJsonObject());

		cond.execute(env);
	}


	@Test
	public void testEvaluate_caseInvalidRequestObject() {
		env.putInteger("token_endpoint_response_http_status", 400);
		env.putObject("token_endpoint_response", JsonParser.parseString("{\"error\":\"invalid_request_object\"}").getAsJsonObject());

		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseBadHttpStatusInvalidRequest() {
		assertThrows(ConditionError.class, () -> {
			env.putInteger("token_endpoint_response_http_status", 401);
			env.putObject("token_endpoint_response", JsonParser.parseString("{\"error\":\"invalid_request\"}").getAsJsonObject());

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_caseBadHttpStatusInvalidClient() {
		assertThrows(ConditionError.class, () -> {
			env.putInteger("token_endpoint_response_http_status", 402);
			env.putObject("token_endpoint_response", JsonParser.parseString("{\"error\":\"invalid_client\"}").getAsJsonObject());

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_caseBadHttpStatus200InvalidClient() {
		assertThrows(ConditionError.class, () -> {
			env.putInteger("token_endpoint_response_http_status", 200);
			env.putObject("token_endpoint_response", JsonParser.parseString("{\"error\":\"invalid_client\"}").getAsJsonObject());

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_caseBadHttpErrorInvalidRequestObject() {
		assertThrows(ConditionError.class, () -> {
			env.putInteger("token_endpoint_response_http_status", 401);
			env.putObject("token_endpoint_response", JsonParser.parseString("{\"error\":\"invalid_request_object\"}").getAsJsonObject());

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_caseBadErrorAccessDenied() {
		assertThrows(ConditionError.class, () -> {
			env.putInteger("token_endpoint_response_http_status", 401);
			env.putObject("token_endpoint_response", JsonParser.parseString("{\"error\":\"access_denied\"}").getAsJsonObject());

			cond.execute(env);
		});
	}
}
