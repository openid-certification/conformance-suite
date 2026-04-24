package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckRequestUriMethodParameter_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckRequestUriMethodParameter cond;

	@BeforeEach
	public void setUp() {
		cond = new CheckRequestUriMethodParameter();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_acceptsPostFromHttpRequest() {
		JsonObject httpRequestParams = new JsonObject();
		httpRequestParams.addProperty("request_uri_method", "post");
		env.putObject("authorization_endpoint_http_request_params", httpRequestParams);

		cond.execute(env);

		assertEquals("post", env.getString("request_uri_method"));
	}

	@Test
	public void testEvaluate_ignoresRequestObjectOnlyValue() {
		env.putObject("authorization_endpoint_http_request_params", new JsonObject());

		JsonObject requestObjectClaims = new JsonObject();
		requestObjectClaims.addProperty("request_uri_method", "post");
		JsonObject requestObject = new JsonObject();
		requestObject.add("claims", requestObjectClaims);
		env.putObject("authorization_request_object", requestObject);

		cond.execute(env);

		assertNull(env.getString("request_uri_method"));
	}

	@Test
	public void testEvaluate_rejectsUnknownHttpRequestValue() {
		JsonObject httpRequestParams = new JsonObject();
		httpRequestParams.addProperty("request_uri_method", "patch");
		env.putObject("authorization_endpoint_http_request_params", httpRequestParams);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
