package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.logging.LoggingRequestInterceptor;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Sends real requests to a local HTTP server, so the headers and body checked are the ones
 * that reach the wire after Spring's message converters and the condition's interceptors.
 */
class CallAuthzenApiEndpoint_UnitTest {

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private HttpServer server;

	private final AtomicReference<String> receivedMethod = new AtomicReference<>();

	private final AtomicReference<List<String>> receivedContentType = new AtomicReference<>();

	private final AtomicReference<String> receivedRequestId = new AtomicReference<>();

	private final AtomicReference<String> receivedBody = new AtomicReference<>();

	/** Uses the same HTTP client library as the suite, without TLS or mTLS set-up. */
	private static class LocalCall extends CallAuthzenApiEndpoint {
		@Override
		protected RestTemplate createRestTemplate(Environment env) {
			return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
		}
	}

	/**
	 * Adds interceptors in the order AbstractCondition.createRestTemplate does: an outermost one,
	 * standing in for the deadline interceptor, then the logging interceptor. Each records the
	 * Content-Type it sees.
	 */
	private static final class LocalCallWithInterceptors extends LocalCall {
		private final List<ClientHttpRequestInterceptor> interceptors;

		private LocalCallWithInterceptors(List<ClientHttpRequestInterceptor> interceptors) {
			this.interceptors = interceptors;
		}

		@Override
		protected RestTemplate createRestTemplate(Environment env) {
			RestTemplate restTemplate = super.createRestTemplate(env);
			restTemplate.getInterceptors().addAll(interceptors);
			return restTemplate;
		}
	}

	private static ClientHttpRequestInterceptor recording(AtomicReference<String> seen) {
		return (request, body, execution) -> {
			seen.set(String.valueOf(request.getHeaders().getContentType()));
			return execution.execute(request, body);
		};
	}

	@BeforeEach
	public void setUp() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			receivedMethod.set(exchange.getRequestMethod());
			receivedContentType.set(exchange.getRequestHeaders().get("Content-Type"));
			receivedRequestId.set(exchange.getRequestHeaders().getFirst("X-Request-ID"));
			receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] response = "{\"decision\":true}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, response.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(response);
			}
		});
		server.start();
		env.putString("authzen_api_endpoint", "http://127.0.0.1:" + server.getAddress().getPort() + "/access/v1/evaluation");
		env.putObject("authzen_api_endpoint_request", JsonParser.parseString("""
			{ "subject": { "type": "user", "id": "alice" }, "action": { "name": "read" },
			  "resource": { "type": "record", "id": "record-1" } }""").getAsJsonObject());
	}

	@AfterEach
	public void tearDown() {
		server.stop(0);
	}

	private void call() {
		CallAuthzenApiEndpoint cond = new LocalCall();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		cond.execute(env);
	}

	@Test
	public void default_postsJsonWithJsonContentType() {
		call();
		assertEquals("POST", receivedMethod.get());
		assertEquals(List.of("application/json"), receivedContentType.get());
		assertEquals("alice", OIDFJSON.getString(JsonParser.parseString(receivedBody.get()).getAsJsonObject()
			.getAsJsonObject("subject").get("id")));
		JsonObject response = env.getObject("authzen_api_endpoint_response");
		assertEquals(200, OIDFJSON.getInt(response.get("status")));
		assertTrue(OIDFJSON.getBoolean(response.getAsJsonObject("body_json").get("decision")));
	}

	@Test
	public void contentTypeOverride_isSent() {
		env.putString("authzen_api_endpoint_request_content_type", "text/plain");
		call();
		assertEquals(List.of("text/plain"), receivedContentType.get());
	}

	@Test
	public void emptyContentTypeOverride_sendsNoContentTypeHeader() {
		// Without the condition's interceptor, the String message converter adds text/plain.
		env.putString("authzen_api_endpoint_request_content_type", "");
		call();
		assertNull(receivedContentType.get());
	}

	@Test
	public void emptyContentTypeOverride_isRemovedBeforeTheLoggingInterceptor() throws IOException {
		AtomicReference<String> outermostSaw = new AtomicReference<>();
		AtomicReference<String> loggingSaw = new AtomicReference<>();
		LoggingRequestInterceptor logging = mock(LoggingRequestInterceptor.class);
		when(logging.intercept(any(HttpRequest.class), any(byte[].class), any(ClientHttpRequestExecution.class))).thenAnswer(invocation -> {
			HttpRequest request = invocation.getArgument(0);
			loggingSaw.set(String.valueOf(request.getHeaders().getContentType()));
			return invocation.<ClientHttpRequestExecution>getArgument(2).execute(request, invocation.getArgument(1));
		});
		env.putString("authzen_api_endpoint_request_content_type", "");

		CallAuthzenApiEndpoint cond = new LocalCallWithInterceptors(List.of(recording(outermostSaw), logging));
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		cond.execute(env);

		// The outermost interceptor still runs first and sees the converter's Content-Type; the
		// logging interceptor, and the wire, see none.
		assertEquals("text/plain;charset=ISO-8859-1", outermostSaw.get());
		assertEquals("null", loggingSaw.get());
		assertNull(receivedContentType.get());
	}

	@Test
	public void emptyContentTypeOverride_withoutALoggingInterceptor_isRemovedFirst() {
		AtomicReference<String> otherSaw = new AtomicReference<>();
		env.putString("authzen_api_endpoint_request_content_type", "");

		CallAuthzenApiEndpoint cond = new LocalCallWithInterceptors(List.of(recording(otherSaw)));
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		cond.execute(env);

		assertEquals("null", otherSaw.get());
		assertNull(receivedContentType.get());
	}

	@Test
	public void methodOverride_isUsed() {
		env.putString("authzen_api_endpoint_request_method", "PUT");
		call();
		assertEquals("PUT", receivedMethod.get());
	}

	@Test
	public void rawBody_isSentVerbatim() {
		env.putString("authzen_api_endpoint_request_raw_body", "{ not json");
		call();
		assertEquals("{ not json", receivedBody.get());
	}

	@Test
	public void requestHeaders_areAdded() {
		JsonObject headers = new JsonObject();
		headers.addProperty("X-Request-ID", "req-123");
		env.putObject("authzen_api_endpoint_request_headers", headers);
		call();
		assertEquals("req-123", receivedRequestId.get());
	}
}
