package net.openid.conformance.condition.as;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class CreateEffectiveAuthorizationRequestParameters_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateEffectiveAuthorizationRequestParameters cond;
	@BeforeEach
	public void setUp() throws Exception {

		cond = new CreateEffectiveAuthorizationRequestParameters();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

	}

	@Test
	public void testEvaluate_noRequestObject() {
		JsonObject httpReqParams = new JsonObject();
		httpReqParams.addProperty("p1", "123");
		httpReqParams.addProperty("p2", "234");
		httpReqParams.addProperty("p1", "345");
		env.putObject("authorization_endpoint_http_request_params", httpReqParams);

		cond.execute(env);

		JsonObject res = env.getObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY);

		assertEquals(httpReqParams, res);

	}

	@Test
	public void testEvaluate_maxAgeNullInRequestObject() {
		JsonObject httpReqParams = new JsonObject();
		httpReqParams.addProperty("p1", "123");
		httpReqParams.addProperty("p2", "234");
		httpReqParams.addProperty("p1", "345");
		env.putObject("authorization_endpoint_http_request_params", httpReqParams);

		JsonObject requestObject = new JsonObject();
		JsonObject requestObjectClaims = new JsonObject();
		requestObjectClaims.add("max_age", JsonNull.INSTANCE);

		requestObject.add("claims", requestObjectClaims);
		env.putObject("authorization_request_object", requestObject);
		cond.execute(env);

		JsonObject result = env.getObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY);
		//when max_age is null, CreateEffectiveAuthorizationRequestParameters will ignore/remove it
		assertEquals(httpReqParams, result);
	}

	@Test
	public void testEvaluate_maxAgeNullInRequestObjectNotNullInHttpRequest() {
		JsonObject httpReqParams = new JsonObject();
		httpReqParams.addProperty("max_age", 123);
		httpReqParams.addProperty("p2", "234");
		httpReqParams.addProperty("p1", "345");
		env.putObject("authorization_endpoint_http_request_params", httpReqParams);

		JsonObject requestObject = new JsonObject();
		JsonObject requestObjectClaims = new JsonObject();
		requestObjectClaims.add("max_age", JsonNull.INSTANCE);

		requestObject.add("claims", requestObjectClaims);
		env.putObject("authorization_request_object", requestObject);
		cond.execute(env);

		JsonObject result = env.getObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY);
		//when max_age is json null in request object, even when it's non-null in http request
		//CreateEffectiveAuthorizationRequestParameters will ignore/remove it
		httpReqParams.remove("max_age");
		assertEquals(httpReqParams, result);
	}

	@Test
	public void testEvaluate_withRequestObject() {
		JsonObject httpReqParams = new JsonObject();
		httpReqParams.addProperty("p1", "123");
		httpReqParams.addProperty("p2", "234");
		httpReqParams.addProperty("p3", "345");
		httpReqParams.addProperty("max_age", "99");
		env.putObject("authorization_endpoint_http_request_params", httpReqParams);

		JsonObject requestObject = new JsonObject();
		JsonObject requestObjectClaims = new JsonObject();
		requestObjectClaims.addProperty("max_age", 2);
		requestObjectClaims.addProperty("p1", "aaa");

		requestObject.add("claims", requestObjectClaims);
		env.putObject("authorization_request_object", requestObject);

		JsonObject expected = httpReqParams.deepCopy();
		expected.addProperty("max_age", 2);
		expected.addProperty("p1", "aaa");

		cond.execute(env);

		JsonObject res = env.getObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY);

		assertEquals(expected, res);

	}
}
