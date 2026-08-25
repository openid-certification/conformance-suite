package net.openid.conformance.frontchannel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.openid.conformance.frontchannel.PlaywrightBrowserRunner.Settings;
import net.openid.conformance.frontchannel.PlaywrightBrowserRunner.TraceMode;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end check of {@link PlaywrightBrowserRunner} against an in-process HTTP server that plays
 * an authorization server with a login page.
 *
 * <p>Needs a Playwright Chromium (downloaded on first use to Playwright's browser cache), so it is
 * opt-in: {@code mvn test -Dtest=PlaywrightBrowserRunner_SmokeTest -Dplaywright.smoke=true}
 */
@EnabledIfSystemProperty(named = "playwright.smoke", matches = "true")
public class PlaywrightBrowserRunner_SmokeTest {

	private static final String TEST_ID = "PWSMOKE1";

	@TempDir
	Path tracesDir;

	private HttpServer server;
	private String baseUrl;
	private final AtomicReference<String> loginBody = new AtomicReference<>();
	private final AtomicReference<String> postTargetContentType = new AtomicReference<>();
	private final AtomicReference<String> postTargetBody = new AtomicReference<>();

	private TestInstanceEventLog eventLog;
	private BrowserControl browserControl;

	@BeforeEach
	public void setUp() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

		server.createContext("/login", exchange -> {
			if ("POST".equals(exchange.getRequestMethod())) {
				loginBody.set(readBody(exchange));
				exchange.getResponseHeaders().add("Set-Cookie", "session=abc; Path=/");
				redirect(exchange, "/authorize");
			} else {
				html(exchange, "<h1>Login</h1><form method=\"POST\" action=\"/login\">"
					+ "<input id=\"username\" name=\"username\"><input name=\"password\" type=\"password\">"
					+ "<button id=\"login\" type=\"submit\">Login</button></form>");
			}
		});
		server.createContext("/authorize", exchange -> {
			if (!loggedIn(exchange)) {
				redirect(exchange, "/login");
			} else if ("POST".equals(exchange.getRequestMethod())) {
				redirect(exchange, "/callback?code=123");
			} else {
				html(exchange, "<h1>Consent</h1><form method=\"POST\" action=\"/authorize\">"
					+ "<button id=\"authorize\" type=\"submit\">Authorize</button></form>");
			}
		});
		server.createContext("/callback", exchange -> html(exchange, "<div id=\"result\">Done</div>"));
		// an authorization server that takes its time before redirecting back
		server.createContext("/slow", exchange -> {
			if ("POST".equals(exchange.getRequestMethod())) {
				try {
					Thread.sleep(12_000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				redirect(exchange, "/callback?code=slow");
			} else {
				html(exchange, "<form method=\"POST\" action=\"/slow\"><button id=\"go\" type=\"submit\">Go</button></form>");
			}
		});
		server.createContext("/whoami", exchange ->
			html(exchange, "<div id=\"result\">" + (loggedIn(exchange) ? "logged-in" : "anonymous") + "</div>"));
		server.createContext("/post-target", exchange -> {
			postTargetContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
			postTargetBody.set(readBody(exchange));
			html(exchange, "<pre id=\"body\">" + exchange.getRequestMethod() + " " + postTargetBody.get() + "</pre>");
		});
		server.start();

		eventLog = BsonEncoding.testInstanceEventLog();
		browserControl = new BrowserControl(new JsonObject(), TEST_ID, eventLog, null, null);
	}

	@AfterEach
	public void tearDown() {
		server.stop(0);
	}

	private Settings settings() {
		return new Settings("chromium", true, 0, Map.of(), TraceMode.ON_FAILURE, tracesDir.toString());
	}

	private PlaywrightBrowserRunner runner(String url, String method, String tasksJson) {
		JsonArray tasks = JsonParser.parseString(tasksJson).getAsJsonArray();
		return new PlaywrightBrowserRunner(browserControl, TEST_ID, eventLog, settings(),
			new BrowserVisit(url, tasks, null, method, 0));
	}

	@Test
	public void logsInAuthorizesAndCarriesTheSessionToTheNextRunner() throws Exception {
		String result = runner(baseUrl + "/authorize", "GET", """
			[
				{"task": "Login", "match": "*/login*", "commands": [
					["text", "id", "username", "alice"],
					["text", "name", "password", "secret"],
					["click", "id", "nope", "optional"],
					["click", "id", "login"]
				]},
				{"task": "Consent", "match": "*/authorize*", "optional": true, "commands": [
					["click", "id", "authorize"],
					["wait", "contains", "callback", 5]
				]},
				{"task": "Done", "match": "*/callback?code=*", "commands": [
					["wait", "id", "result", 5, "Done"],
					["wait", "match", "code=\\\\d+", 5]
				]}
			]""").call();

		assertThat(result).isEqualTo("web runner exited");
		assertThat(loginBody.get()).isEqualTo("username=alice&password=secret");
		assertThat(browserControl.getVisited()).containsExactly(baseUrl + "/authorize");
		assertThat(browserControl.getPlaywrightStorageState()).contains("session");
		assertThat(Files.list(tracesDir)).isEmpty(); // on-failure only

		// the cookie set by the login above is seen by a new runner (and hence a new browser)
		runner(baseUrl + "/whoami", "GET", """
			[{"task": "Check", "commands": [["wait", "css", "#result", 5, "^logged-in$"]]}]""").call();
	}

	@Test
	public void clickWaitsForASlowNavigationLikeHtmlUnitDid() throws Exception {
		long start = System.currentTimeMillis();
		runner(baseUrl + "/slow", "GET", """
			[
				{"task": "Go", "match": "*/slow", "commands": [["click", "id", "go"]]},
				{"task": "Done", "match": "*/callback?code=slow", "commands": [["wait", "id", "result", 5, "Done"]]}
			]""").call();

		assertThat(System.currentTimeMillis() - start).isGreaterThanOrEqualTo(12_000);
	}

	@Test
	public void postsTheQueryStringAsFormBody() throws Exception {
		runner(baseUrl + "/post-target?foo=bar%20baz&x=1&y=a%26b", "POST", """
			[{"task": "Check", "match": "*/post-target", "commands": [["wait", "id", "body", 5, "^POST "]]}]""").call();

		assertThat(postTargetContentType.get()).startsWith("application/x-www-form-urlencoded");
		assertThat(postTargetBody.get()).isEqualTo("foo=bar+baz&x=1&y=a%26b");
	}

	@Test
	public void failsAndSavesTraceWhenAnElementNeverAppears() {
		assertThatThrownBy(() -> runner(baseUrl + "/callback", "GET", """
			[{"task": "Wait", "commands": [["wait-element-visible", "id", "missing", 1]]}]""").call())
			.isInstanceOf(TestFailureException.class)
			.hasMessageContaining("Timed out waiting for element visibility");

		assertThat(tracesDir.resolve(TEST_ID + ".zip")).isRegularFile();
	}

	@Test
	public void stopsQuietlyWhenInterruptedBecauseTheTestFinished() throws Exception {
		PlaywrightBrowserRunner runner = runner(baseUrl + "/callback", "GET", """
			[{"task": "Wait", "commands": [["wait", "id", "never-appears", 30]]}]""");
		AtomicReference<Object> outcome = new AtomicReference<>();
		Thread thread = new Thread(() -> {
			try {
				outcome.set(runner.call());
			} catch (Throwable t) {
				outcome.set(t);
			}
		});
		thread.start();

		// once the runner is blocked on its wait command, cancel it the way TestExecutionManager does
		long deadline = System.currentTimeMillis() + 30_000;
		while (!"wait".equals(currentCommand(runner)) && System.currentTimeMillis() < deadline) {
			Thread.sleep(100);
		}
		assertThat(currentCommand(runner)).isEqualTo("wait");
		thread.interrupt();
		thread.join(15_000);

		assertThat(thread.isAlive()).as("runner thread finished").isFalse();
		assertThat(outcome.get()).isEqualTo("web runner cancelled");
		assertThat(OIDFJSON.getString(runner.getStatus().get("lastException"))).isEqualTo("Stopped as the test has finished");
		assertThat(Files.list(tracesDir)).as("cancellation is not a failure, so no trace").isEmpty();
	}

	private static String currentCommand(PlaywrightBrowserRunner runner) {
		JsonElement command = runner.getStatus().get("currentCommand");
		return command == null || command.isJsonNull() ? null : OIDFJSON.getString(command);
	}

	@Test
	public void failsOnUnexpectedUrlForNonOptionalTask() {
		assertThatThrownBy(() -> runner(baseUrl + "/callback", "GET", """
			[{"task": "Elsewhere", "match": "*/somewhere-else*", "commands": [["click", "id", "x"]]}]""").call())
			.isInstanceOf(TestFailureException.class)
			.hasMessageContaining("unexpected url for task: Elsewhere");
	}

	private static boolean loggedIn(HttpExchange exchange) {
		String cookie = exchange.getRequestHeaders().getFirst("Cookie");
		return cookie != null && cookie.contains("session=abc");
	}

	private static String readBody(HttpExchange exchange) throws IOException {
		return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
	}

	private static void redirect(HttpExchange exchange, String location) throws IOException {
		exchange.getResponseHeaders().add("Location", location);
		exchange.sendResponseHeaders(302, -1);
		exchange.close();
	}

	private static void html(HttpExchange exchange, String body) throws IOException {
		byte[] bytes = ("<!DOCTYPE html><html><body>" + body + "</body></html>").getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
		exchange.sendResponseHeaders(200, bytes.length);
		try (OutputStream out = exchange.getResponseBody()) {
			out.write(bytes);
		}
	}
}
