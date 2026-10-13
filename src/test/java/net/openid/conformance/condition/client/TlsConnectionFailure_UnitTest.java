package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import javax.net.ssl.SSLHandshakeException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TlsConnectionFailure_UnitTest {

	@Test
	public void sslExceptionIsFailure() {
		assertThat(TlsConnectionFailure.isTlsConnectionFailure(
			new ResourceAccessException("x", new SSLHandshakeException("bad_certificate")))).isTrue();
	}

	@Test
	public void socketExceptionIsFailure() {
		assertThat(TlsConnectionFailure.isTlsConnectionFailure(
			new ResourceAccessException("x", new SocketException("Connection reset")))).isTrue();
	}

	@Test
	public void noHttpResponseIsFailure() {
		assertThat(TlsConnectionFailure.isTlsConnectionFailure(
			new ResourceAccessException("x", new NoHttpResponseException("failed to respond")))).isTrue();
	}

	@Test
	public void connectionRefusedIsNotFailure() {
		assertThat(TlsConnectionFailure.isTlsConnectionFailure(
			new ResourceAccessException("x", new ConnectException("Connection refused")))).isFalse();
	}

	@Test
	public void noRouteToHostIsNotFailure() {
		assertThat(TlsConnectionFailure.isTlsConnectionFailure(
			new ResourceAccessException("x", new NoRouteToHostException("No route to host")))).isFalse();
	}

	@Test
	public void otherIOExceptionIsNotFailure() {
		assertThat(TlsConnectionFailure.isTlsConnectionFailure(
			new ResourceAccessException("x", new IOException("other")))).isFalse();
	}

	@Test
	public void nonResourceAccessExceptionIsNotFailure() {
		assertThat(TlsConnectionFailure.isTlsConnectionFailure(
			new RestClientException("x", new SocketException("Connection reset")))).isFalse();
	}

	/**
	 * A server that reads the request then closes the connection without responding, as a Google Cloud load
	 * balancer in mTLS REJECT_INVALID mode does after the TLS handshake when no client certificate was sent.
	 */
	@Test
	public void tokenEndpointClosingConnectionIsTreatedAsRejection() throws Exception {
		try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
			Thread serverThread = new Thread(() -> {
				try (Socket socket = server.accept()) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
					String line;
					do {
						line = reader.readLine();
					} while (line != null && !line.isEmpty());
				} catch (IOException e) {
					// the client sees the closed connection either way
				}
			});
			serverThread.start();

			Environment env = tokenEndpointEnv("http://127.0.0.1:" + server.getLocalPort() + "/token");
			newCondition().execute(env);
			serverThread.join(10_000);

			assertThat(env.getBoolean("token_endpoint_response_ssl_error")).isTrue();
			assertThat(env.getObject("token_endpoint_response_full")).isNull();
		}
	}

	@Test
	public void tokenEndpointRefusingConnectionIsNotTreatedAsRejection() throws Exception {
		int port;
		try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
			port = server.getLocalPort();
		}
		Environment env = tokenEndpointEnv("http://127.0.0.1:" + port + "/token");
		assertThrows(ConditionError.class, () -> newCondition().execute(env));
	}

	private static Environment tokenEndpointEnv(String tokenEndpoint) {
		Environment env = new Environment();
		env.putString("server", "token_endpoint", tokenEndpoint);
		JsonObject form = new JsonObject();
		form.addProperty("grant_type", "client_credentials");
		env.putObject("token_endpoint_request_form_parameters", form);
		return env;
	}

	private static CallTokenEndpointAllowingTLSFailure newCondition() {
		CallTokenEndpointAllowingTLSFailure cond = new CallTokenEndpointAllowingTLSFailure() {
			@Override
			protected int getHttpClientTimeoutSeconds() {
				return 5;
			}
		};
		cond.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.INFO);
		return cond;
	}
}
