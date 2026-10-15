package net.openid.conformance.authzen.condition;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallAuthzenApiEndpointAllowingTlsRejection_UnitTest {

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CallAuthzenApiEndpointAllowingTlsRejection cond;

	/** Uses the same HTTP client library as the suite, without the suite's TLS or mTLS set-up. */
	private static final class LocalCall extends CallAuthzenApiEndpointAllowingTlsRejection {
		@Override
		protected RestTemplate createRestTemplate(Environment env) {
			return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
		}
	}

	@BeforeEach
	public void setUp() {
		cond = new LocalCall();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env.putObject("authzen_api_endpoint_request", JsonParser.parseString("""
			{ "subject": { "type": "user", "id": "alice" }, "action": { "name": "read" },
			  "resource": { "type": "record", "id": "record-1" } }""").getAsJsonObject());
		// A response left by an earlier call must not be mistaken for an answer to this one.
		env.putObject("authzen_api_endpoint_response", JsonParser.parseString("{ \"status\": 200 }").getAsJsonObject());
	}

	private HttpServer server;

	@AfterEach
	public void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	public void handshakeClosedByServer_isATlsRejection() throws IOException {
		// The server accepts the connection and closes it during the TLS handshake, as a PDP does
		// when it refuses a client without an acceptable certificate.
		try (ServerSocket socket = new ServerSocket(0)) {
			Thread acceptor = new Thread(() -> {
				try (Socket client = socket.accept()) {
					client.getInputStream().read();
				} catch (IOException e) {
					// the test only needs the connection closed
				}
			});
			acceptor.start();
			env.putString("authzen_api_endpoint", "https://127.0.0.1:" + socket.getLocalPort() + "/access/v1/evaluation");

			cond.execute(env);

			assertTrue(env.getBoolean(CallAuthzenApiEndpointAllowingTlsRejection.TLS_REJECTED_KEY));
			assertNull(env.getObject("authzen_api_endpoint_response"));
		}
	}

	@Test
	public void httpAnswer_isNotATlsRejection() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			exchange.getRequestBody().readAllBytes();
			byte[] response = "{\"error\":\"unauthorized\"}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(401, response.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(response);
			}
		});
		server.start();
		env.putString("authzen_api_endpoint", "http://127.0.0.1:" + server.getAddress().getPort() + "/access/v1/evaluation");

		cond.execute(env);

		assertFalse(env.getBoolean(CallAuthzenApiEndpointAllowingTlsRejection.TLS_REJECTED_KEY));
		assertEquals(401, env.getInteger("authzen_api_endpoint_response", "status"));
	}

	@Test
	public void refusedConnection_fails() throws IOException {
		// Nothing listens on a port just released, so no request reached a server: not a rejection.
		int port;
		try (ServerSocket socket = new ServerSocket(0)) {
			port = socket.getLocalPort();
		}
		env.putString("authzen_api_endpoint", "https://127.0.0.1:" + port + "/access/v1/evaluation");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
