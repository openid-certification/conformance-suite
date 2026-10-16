package net.openid.conformance.authzen.condition;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the well-known discovery URL derivation (the
 * {@code /.well-known/authzen-configuration} segment is inserted between the host
 * and any existing path, not appended after the whole URL) and the parsing of the
 * metadata response body, and the failure reported when the metadata endpoint answers
 * with an error status.
 */
class GetPDPDynamicServerConfiguration_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private GetPDPDynamicServerConfiguration cond;

	@BeforeEach
	public void setUp() {
		cond = new GetPDPDynamicServerConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void noPath_appendsWellKnown() {
		assertEquals("https://pdp.example.com/.well-known/authzen-configuration",
			cond.deriveDiscoveryUrl("https://pdp.example.com"));
	}

	@Test
	public void trailingSlashOnly_appendsWellKnown() {
		assertEquals("https://pdp.example.com/.well-known/authzen-configuration",
			cond.deriveDiscoveryUrl("https://pdp.example.com/"));
	}

	@Test
	public void tenantPath_insertsWellKnownBeforePath() {
		assertEquals("https://pdp.example.com/.well-known/authzen-configuration/tenant1",
			cond.deriveDiscoveryUrl("https://pdp.example.com/tenant1"));
	}

	@Test
	public void tenantPathTrailingSlash_insertsWellKnownAndStripsTrailingSlash() {
		assertEquals("https://pdp.example.com/.well-known/authzen-configuration/tenant1",
			cond.deriveDiscoveryUrl("https://pdp.example.com/tenant1/"));
	}

	@Test
	public void nestedPath_insertsWellKnownBeforeFullPath() {
		assertEquals("https://pdp.example.com/.well-known/authzen-configuration/a/b",
			cond.deriveDiscoveryUrl("https://pdp.example.com/a/b"));
	}

	@Test
	public void hostWithPort_isPreserved() {
		assertEquals("https://pdp.example.com:8443/.well-known/authzen-configuration/tenant1",
			cond.deriveDiscoveryUrl("https://pdp.example.com:8443/tenant1"));
	}

	@Test
	public void invalidUrl_fails() {
		assertThrows(ConditionError.class, () -> cond.deriveDiscoveryUrl("ht tp://bad url"));
	}

	@Test
	public void relativeUrlWithoutSchemeOrHost_fails() {
		assertThrows(ConditionError.class, () -> cond.deriveDiscoveryUrl("/tenant1"));
	}
	@Test
	public void objectBody_isParsed() {
		assertEquals("https://pdp.example.com",
			OIDFJSON.getString(cond.parseServerConfiguration("{\"policy_decision_point\":\"https://pdp.example.com\"}").get("policy_decision_point")));
	}

	@Test
	public void arrayBody_fails() {
		Throwable e = assertThrows(ConditionError.class, () -> cond.parseServerConfiguration("[]"));
		assertTrue(e.getMessage().contains("not a JSON object"));
	}

	@Test
	public void stringBody_fails() {
		Throwable e = assertThrows(ConditionError.class, () -> cond.parseServerConfiguration("\"metadata\""));
		assertTrue(e.getMessage().contains("not a JSON object"));
	}

	@Test
	public void malformedBody_fails() {
		assertThrows(ConditionError.class, () -> cond.parseServerConfiguration("{"));
	}

	@Test
	public void emptyBody_fails() {
		assertThrows(ConditionError.class, () -> cond.parseServerConfiguration(""));
	}

	/** Uses the same HTTP client library as the suite, without TLS set-up. */
	private static final class LocalFetch extends GetPDPDynamicServerConfiguration {
		@Override
		protected RestTemplate createRestTemplate(Environment env) {
			return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
		}
	}

	/** Fetches the metadata from a local server that answers every request with the given status. */
	private ConditionError fetchFromServerAnswering(int status) throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			byte[] response = "{\"error\":\"not here\"}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, response.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(response);
			}
		});
		server.start();
		try {
			Environment env = new Environment();
			env.putObject("config", JsonParser.parseString("{\"pdp\": {\"policy_decision_point\": \"http://127.0.0.1:"
				+ server.getAddress().getPort() + "\"}}").getAsJsonObject());
			GetPDPDynamicServerConfiguration fetch = new LocalFetch();
			fetch.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
			return assertThrows(ConditionError.class, () -> fetch.execute(env));
		} finally {
			server.stop(0);
		}
	}

	@Test
	public void notFound_failsNamingTheStatus() throws IOException {
		ConditionError e = fetchFromServerAnswering(404);
		assertTrue(e.getMessage().contains("/.well-known/authzen-configuration: the PDP answered HTTP 404"), e.getMessage());
	}

	@Test
	public void serverError_failsNamingTheStatus() throws IOException {
		ConditionError e = fetchFromServerAnswering(500);
		assertTrue(e.getMessage().contains("the PDP answered HTTP 500"), e.getMessage());
	}
}
