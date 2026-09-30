package net.openid.conformance.openid.ssf.conditions;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class AbstractOIDSSFTransmitterEndpointCall_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private static class TestEndpointCall extends AbstractOIDSSFTransmitterEndpointCall {
		@Override
		protected String getEndpointName() {
			return "unit-test endpoint";
		}

		@Override
		protected String getResourceEndpointUrl(Environment env) {
			return "https://transmitter.example/unit-test";
		}
	}

	private TestEndpointCall createCondition() {
		TestEndpointCall condition = new TestEndpointCall();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private static HttpClientErrorException clientError(HttpStatus status, String contentType, String body) {
		HttpHeaders headers = null;
		if (contentType != null) {
			headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(contentType));
			headers.set("WWW-Authenticate", "Bearer error=\"invalid_token\"");
		}
		return HttpClientErrorException.create(status, status.getReasonPhrase(), headers,
			body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
	}

	private JsonObject recordedResponse() {
		JsonObject response = env.getObject("resource_endpoint_response_full");
		assertNotNull(response);
		return response;
	}

	@Test
	void recordsAParsedJsonErrorBody() {
		HttpClientErrorException e = clientError(HttpStatus.UNAUTHORIZED, "application/json", "{\"error\":\"invalid_token\"}");
		assertDoesNotThrow(() -> createCondition().handleClientResponseException(env, e));

		JsonObject response = recordedResponse();
		assertEquals(401, OIDFJSON.getInt(response.get("status")));
		assertEquals("invalid_token", OIDFJSON.getString(response.getAsJsonObject("body_json").get("error")));
		assertTrue(response.has("headers"));
	}

	@Test
	void recordsAVendorJsonContentType() {
		HttpClientErrorException e = clientError(HttpStatus.BAD_REQUEST, "application/vnd.example+json", "{\"err\":\"bad\"}");
		assertDoesNotThrow(() -> createCondition().handleClientResponseException(env, e));
		assertEquals("bad", OIDFJSON.getString(recordedResponse().getAsJsonObject("body_json").get("err")));
	}

	@Test
	void toleratesAJsonContentTypeWithAnUnparseableBody() {
		HttpClientErrorException e = clientError(HttpStatus.UNAUTHORIZED, "application/json", "Unauthorized");
		assertDoesNotThrow(() -> createCondition().handleClientResponseException(env, e));

		JsonObject response = recordedResponse();
		assertEquals(401, OIDFJSON.getInt(response.get("status")));
		assertEquals("Unauthorized", OIDFJSON.getString(response.get("body")));
		assertFalse(response.has("body_json"));
		assertTrue(response.has("headers"));
	}

	@Test
	void toleratesAJsonContentTypeWithAMalformedObject() {
		HttpClientErrorException e = clientError(HttpStatus.BAD_REQUEST, "application/json", "{\"err\":");
		assertDoesNotThrow(() -> createCondition().handleClientResponseException(env, e));
		assertFalse(recordedResponse().has("body_json"));
	}

	@Test
	void toleratesAJsonContentTypeWithAnEmptyBody() {
		HttpClientErrorException e = clientError(HttpStatus.UNAUTHORIZED, "application/json", "");
		assertDoesNotThrow(() -> createCondition().handleClientResponseException(env, e));
		assertFalse(recordedResponse().has("body_json"));
	}

	@Test
	void skipsTheJsonBodyForOtherContentTypes() {
		HttpClientErrorException e = clientError(HttpStatus.FORBIDDEN, "text/plain", "{\"looks\":\"like json\"}");
		assertDoesNotThrow(() -> createCondition().handleClientResponseException(env, e));
		assertFalse(recordedResponse().has("body_json"));
	}

	@Test
	void toleratesAResponseWithoutHeaders() {
		HttpClientErrorException e = clientError(HttpStatus.NOT_FOUND, null, "");
		assertDoesNotThrow(() -> createCondition().handleClientResponseException(env, e));

		JsonObject response = recordedResponse();
		assertEquals(404, OIDFJSON.getInt(response.get("status")));
		assertFalse(response.has("headers"));
		assertFalse(response.has("body_json"));
	}
}
