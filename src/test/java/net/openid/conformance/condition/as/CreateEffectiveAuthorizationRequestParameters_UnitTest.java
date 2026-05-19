package net.openid.conformance.condition.as;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import net.openid.conformance.testmodule.OIDFJSON;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class CreateEffectiveAuthorizationRequestParameters_UnitTest {
	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

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
		// JsonNull from request object is preserved so downstream conditions
		// (e.g. EnsureNumericRequestObjectClaimsAreNotNull) can flag it.
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
		assertTrue(result.has("max_age") && result.get("max_age").isJsonNull(),
			"max_age JsonNull from request object should be preserved");
	}

	@Test
	public void testEvaluate_maxAgeNullInRequestObjectNotNullInHttpRequest() {
		// Request object's JsonNull overrides the HTTP param value and is preserved.
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
		assertTrue(result.has("max_age") && result.get("max_age").isJsonNull(),
			"max_age JsonNull from request object should override HTTP param and be preserved");
	}

	@Test
	public void testEvaluate_authorizationDetailsStringParsedToJson() {
		// convertJsonStringParam parses JSON string params into their JSON representation
		// without enforcing type (e.g. array vs object); array type validation for
		// authorization_details (per RFC 9396) is handled downstream by
		// RARSupport.EnsureEffectiveAuthorizationEndpointRequestContainsValidRAR
		// via getJsonArrayFromEnvironment.
		JsonObject httpReqParams = new JsonObject();
		httpReqParams.addProperty("authorization_details", "[{\"type\":\"openid_credential\"}]");
		env.putObject("authorization_endpoint_http_request_params", httpReqParams);

		cond.execute(env);

		JsonObject res = env.getObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY);
		assertTrue(res.get("authorization_details").isJsonArray(),
			"authorization_details JSON array string should be parsed into a JsonArray");
	}

	@Test
	public void testEvaluate_authorizationDetailsObjectStringAccepted() {
		// An object-valued authorization_details string should be parsed without error;
		// array type validation (per RFC 9396) is handled downstream by
		// RARSupport.EnsureEffectiveAuthorizationEndpointRequestContainsValidRAR.
		JsonObject httpReqParams = new JsonObject();
		httpReqParams.addProperty("authorization_details", "{\"type\":\"openid_credential\"}");
		env.putObject("authorization_endpoint_http_request_params", httpReqParams);

		cond.execute(env);

		JsonObject res = env.getObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY);
		assertTrue(res.get("authorization_details").isJsonObject(),
			"authorization_details JSON object string should be parsed into a JsonObject");
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

	@Test
	public void testEvaluate_maxAgeStringInRequestObjectPreserved() {
		// When the request object sends max_age as a string (a protocol violation), the
		// effective value should remain a string so downstream conditions can flag it.
		// The numeric normalization only applies to HTTP query parameters.
		JsonObject httpReqParams = new JsonObject();
		httpReqParams.addProperty("p1", "123");
		env.putObject("authorization_endpoint_http_request_params", httpReqParams);

		JsonObject requestObject = new JsonObject();
		JsonObject requestObjectClaims = new JsonObject();
		requestObjectClaims.addProperty("max_age", "99"); // string, not number
		requestObject.add("claims", requestObjectClaims);
		env.putObject("authorization_request_object", requestObject);

		cond.execute(env);

		JsonObject res = env.getObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY);
		assertTrue(res.get("max_age").isJsonPrimitive() && res.get("max_age").getAsJsonPrimitive().isString(),
			"max_age from request object should remain a string");
		assertEquals("99", OIDFJSON.getString(res.get("max_age")));
	}
}
