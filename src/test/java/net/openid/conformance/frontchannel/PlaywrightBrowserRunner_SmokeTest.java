package net.openid.conformance.frontchannel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
	private final AtomicReference<String> formPostCallbackBody = new AtomicReference<>();

	private TestInstanceEventLog eventLog;
	private BrowserControl browserControl;

	@BeforeEach
	public void setUp() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

		// the login page returns to the authorization endpoint it was sent from ('return' query parameter)
		server.createContext("/login", exchange -> {
			String query = exchange.getRequestURI().getQuery();
			String returnTo = query != null && query.startsWith("return=") ? query.substring("return=".length()) : "/authorize";
			if ("POST".equals(exchange.getRequestMethod())) {
				loginBody.set(readBody(exchange));
				exchange.getResponseHeaders().add("Set-Cookie", "session=abc; Path=/");
				redirect(exchange, returnTo);
			} else {
				html(exchange, "<h1>Login</h1><form method=\"POST\" action=\"/login?return=" + returnTo + "\">"
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
		// like /authorize, but redirecting back to a callback that takes its time to answer, as the
		// suite does when it is busy processing the authorization response
		server.createContext("/slow-authorize", exchange -> {
			if (!loggedIn(exchange)) {
				redirect(exchange, "/login?return=/slow-authorize");
			} else if ("POST".equals(exchange.getRequestMethod())) {
				redirect(exchange, "/slow-callback?code=789");
			} else {
				html(exchange, "<h1>Consent</h1><form method=\"POST\" action=\"/slow-authorize\">"
					+ "<button id=\"authorize\" type=\"submit\">Authorize</button></form>");
			}
		});
		server.createContext("/slow-callback", exchange -> {
			try {
				Thread.sleep(8_000); // comfortably longer than launching a browser takes
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			html(exchange, "<div id=\"result\">Done</div>");
		});
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
		// an authorization server answering the consent with a self-submitting form (response_mode=form_post),
		// posting to a callback that takes its time to answer, as the suite does when busy
		server.createContext("/form-post-authorize", exchange -> {
			if ("POST".equals(exchange.getRequestMethod())) {
				redirect(exchange, "/form-post-response");
			} else {
				html(exchange, "<h1>Consent</h1><form method=\"POST\" action=\"/form-post-authorize\">"
					+ "<button id=\"authorize\" type=\"submit\">Authorize</button></form>");
			}
		});
		server.createContext("/form-post-response", exchange ->
			html(exchange, "<form method=\"POST\" action=\"/form-post-callback\"><input type=\"hidden\" name=\"code\" value=\"456\"></form>"
				+ "<script>window.addEventListener('load', function() { document.forms[0].submit(); });</script>"));
		server.createContext("/form-post-callback", exchange -> {
			try {
				Thread.sleep(2_000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			formPostCallbackBody.set(readBody(exchange));
			html(exchange, "<div id=\"result\">Done</div>");
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
		return settings(true);
	}

	private Settings settings(boolean sharedBrowser) {
		return new Settings("chromium", true, 0, sharedBrowser, Map.of(), TraceMode.ON_FAILURE, tracesDir.toString());
	}

	private PlaywrightBrowserRunner runner(String url, String method, String tasksJson) {
		return runner(url, method, tasksJson, settings());
	}

	private PlaywrightBrowserRunner runner(String url, String method, String tasksJson, Settings settings) {
		JsonArray tasks = JsonParser.parseString(tasksJson).getAsJsonArray();
		return new PlaywrightBrowserRunner(browserControl, TEST_ID, eventLog, settings,
			new BrowserVisit(url, tasks, null, method, 0));
	}

	@Test
	public void runnersShareTheJvmsBrowserAndKeepTheirSessionsApart() throws Exception {
		// the first runner logs in, so its context has the session cookie...
		runner(baseUrl + "/authorize", "GET", """
			[
				{"task": "Login", "match": "*/login*", "commands": [["text", "id", "username", "alice"], ["text", "name", "password", "secret"], ["click", "id", "login"]]},
				{"task": "Consent", "match": "*/authorize*", "commands": [["click", "id", "authorize"]]},
				{"task": "Done", "match": "*/callback?code=*", "commands": [["wait", "id", "result", 5, "Done"]]}
			]""").call();
		assertThat(PlaywrightBrowserServer.isRunning()).as("shared browser server running").isTrue();
		String endpoint = PlaywrightBrowserServer.endpoint(settings());

		// ...which a runner of another test (another BrowserControl) in the same browser must not see
		BrowserControl otherTest = new BrowserControl(new JsonObject(), "PWSMOKE2", eventLog, null, null);
		JsonArray tasks = JsonParser.parseString("""
			[{"task": "Check", "commands": [["wait", "css", "#result", 5, "^anonymous$"]]}]""").getAsJsonArray();
		new PlaywrightBrowserRunner(otherTest, "PWSMOKE2", eventLog, settings(),
			new BrowserVisit(baseUrl + "/whoami", tasks, null, "GET", 0)).call();

		// while the same test's next runner does, through the storage state as before
		runner(baseUrl + "/whoami", "GET", """
			[{"task": "Check", "commands": [["wait", "css", "#result", 5, "^logged-in$"]]}]""").call();

		assertThat(PlaywrightBrowserServer.endpoint(settings())).as("same server throughout").isEqualTo(endpoint);
	}

	@Test
	public void skipsOptionalTasksWithoutWaitingWhenAlreadyOnALaterTasksPage() throws Exception {
		// a user the authorization server still knows is sent straight to the callback: the optional
		// login and consent tasks must be skipped at once, not after their url wait each (2s)
		long start = System.currentTimeMillis();
		runner(baseUrl + "/callback?code=1", "GET", """
			[
				{"task": "Login", "optional": true, "match": "*/login*", "commands": [["click", "id", "login"]]},
				{"task": "Consent", "optional": true, "match": "*/authorize*", "commands": [["click", "id", "authorize"]]},
				{"task": "Done", "match": "*/callback*", "commands": [["wait", "id", "result", 5, "Done"]]}
			]""").call();

		assertThat(System.currentTimeMillis() - start).as("no 2s wait per skipped optional task").isLessThan(3_500);
	}

	@Test
	public void runnerLaunchesItsOwnBrowserWhenSharingIsOff() throws Exception {
		String result = runner(baseUrl + "/whoami", "GET", """
			[{"task": "Check", "commands": [["wait", "css", "#result", 5, "^anonymous$"]]}]""", settings(false)).call();

		assertThat(result).isEqualTo("web runner exited");
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
	public void waitsForTheEarlierRunnerToFinishSoItsSessionIsCarriedOver() throws Exception {
		// The suite starts the next scripted browser run as soon as it has processed the callback the
		// previous browser was redirected to, i.e. while that browser is still loading the callback
		// page and has not saved its session state yet (this is how the second authorization of the
		// prompt=none / id_token_hint / max_age tests is started). The next runner must not start its
		// browser until the previous one has finished, or the login is lost.
		PlaywrightBrowserRunner first = runner(baseUrl + "/slow-authorize", "GET", """
			[
				{"task": "Login", "match": "*/login*", "commands": [
					["text", "id", "username", "alice"],
					["text", "name", "password", "secret"],
					["click", "id", "login"]
				]},
				{"task": "Consent", "match": "*/slow-authorize*", "commands": [["click", "id", "authorize"]]},
				{"task": "Done", "match": "*/slow-callback*", "commands": [["wait", "id", "result", 5, "Done"]]}
			]""");
		PlaywrightBrowserRunner second = runner(baseUrl + "/whoami", "GET", """
			[{"task": "Check", "commands": [["wait", "css", "#result", 5, "^logged-in$"]]}]""");
		// registered in submission order, as BrowserControl.goToUrl does
		browserControl.addRunner(first);
		browserControl.addRunner(second);

		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<String> firstResult = executor.submit(first);

			// once the first browser has logged in it is on its way to the slow callback; start the second now
			long deadline = System.currentTimeMillis() + 30_000;
			while (loginBody.get() == null && System.currentTimeMillis() < deadline) {
				Thread.sleep(100);
			}
			assertThat(loginBody.get()).as("first runner logged in").isNotNull();
			assertThat(firstResult.isDone()).as("first runner still running").isFalse();
			Future<String> secondResult = executor.submit(second);

			assertThat(firstResult.get(60, TimeUnit.SECONDS)).isEqualTo("web runner exited");
			assertThat(secondResult.get(60, TimeUnit.SECONDS)).isEqualTo("web runner exited");
		} finally {
			executor.shutdownNow();
		}

		assertThat(browserControl.runnersActive()).isFalse();
		assertThat(browserControl.getPlaywrightStorageState()).contains("session");
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
	public void waitsForASelfSubmittingFormPostPageToReachTheCallback() throws Exception {
		// the click returns once the authorization server's form_post page has committed; the POST that page
		// sends from its load handler is still in flight when the following tasks look at the url
		long start = System.currentTimeMillis();
		runner(baseUrl + "/form-post-authorize", "GET", """
			[
				{"task": "Consent", "match": "*/form-post-authorize*", "commands": [["click", "id", "authorize"]]},
				{"task": "Login again", "optional": true, "match": "*/login*", "commands": [["click", "id", "login"]]},
				{"task": "Done", "match": "*/form-post-callback*", "commands": [["wait", "id", "result", 5, "Done"]]}
			]""").call();

		assertThat(formPostCallbackBody.get()).isEqualTo("code=456");
		assertThat(System.currentTimeMillis() - start).isGreaterThanOrEqualTo(2_000);
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
	public void terminateDriverKillsDriverAndBrowser() {
		PlaywrightBrowserRunner.ensureBrowserInstalled("chromium");
		Playwright playwright = Playwright.create();
		Browser browser = playwright.chromium().launch();
		Page page = browser.newContext().newPage();
		page.navigate(baseUrl + "/callback");
		long driverPid = ProcessHandle.current().children()
			.filter(p -> p.info().commandLine().orElse("").contains("playwright"))
			.mapToLong(ProcessHandle::pid).max().orElseThrow();

		PlaywrightBrowserRunner.terminateDriver(TEST_ID, playwright);

		assertThat(ProcessHandle.of(driverPid).map(ProcessHandle::isAlive).orElse(false)).as("driver process alive").isFalse();
		assertThatThrownBy(page::title).isInstanceOf(PlaywrightException.class);
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
