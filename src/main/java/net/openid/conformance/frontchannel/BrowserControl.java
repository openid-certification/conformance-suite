package net.openid.conformance.frontchannel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.info.ImageService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.runner.TestExecutionManager;
import net.openid.conformance.testmodule.DataUtils;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import org.bson.Document;
import org.htmlunit.CookieManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.PatternMatchUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Front channel control for a test module instance: urls the test wants visited are either
 * handed to a scripted browser ({@link BrowserRunner}) when the test configuration's "browser"
 * block has a matching entry, or exposed to the user for manual interaction.
 *
 * <p>The scripted browser engine is selected with the {@code browser.engine} system property:
 * {@code selenium} (default, headless HtmlUnit driven through Selenium, see
 * {@link SeleniumBrowserRunner}) or {@code playwright} (a real Chromium/Firefox/WebKit, see
 * {@link PlaywrightBrowserRunner} and its {@code browser.playwright.*} settings).
 */
public class BrowserControl implements DataUtils {

	/*  EXAMPLE OF WHAT TO ADD TO CONFIG:
	 "browser": [
		{
			"match":"https://mitreid.org/authorize*",
			"tasks": [
				{
					"task": "Initial Login",
					"match": "https://mitreid.org/login*",
					"commands": [
						["text","id","j_username","user"],
						["text","id","j_password","password"],
						["click","name","submit"]
					]
				},
				{
					"task": "Authorize Client",
					"match": "https://mitreid.org/authorize*",
					"optional": true,
					"commands": [
						["click","id","remember-not"],
						["click","name","authorize"],
						["wait", "contains", "localhost", 10] // wait for up to 10 seconds for the URL to contain 'localhost' via a javascript location change, etc.
					]
				},
				{
					"task": "Verify Complete",
					"match": "https://localhost*"
				}
			]
		}
	 ]

	 Each "Task" should be things that happen on a single page. In the above example, the first task logs in and ends
	 with clicking the submit button on the login page, resulting in a new page to get loaded. (The result of logging in).

	 The second task clicks the "Do not remember this choice" radio button, and then clicks the authorize button which
	 then should trigger the redirect from the server.
	 */

	private static final Logger logger = LoggerFactory.getLogger(BrowserControl.class);

	public static final String ENGINE_SELENIUM = "selenium";
	public static final String ENGINE_PLAYWRIGHT = "playwright";

	private String testId;

	private TestExecutionManager executionManager;
	private JsonArray browserCommands = null;
	private boolean verboseLogging;
	private boolean showQrCodes = false;
	private boolean cssParsingEnabled = true;
	private final String engine;

	private List<String> urls = new ArrayList<>();
	private List<UrlWithMethod> urlsWithMethod = new ArrayList<>();
	private List<String> visited = new ArrayList<>();
	private List<UrlWithMethod> visitedUrlsWithMethod = new ArrayList<>();
	private List<BrowserApiRequest> browserApiRequests = new ArrayList<>();
	private List<UriInputRequest> uriInputRequests = new ArrayList<>();
	private Queue<BrowserRunner> runners = new ConcurrentLinkedQueue<>();

	private ImageService imageService;

	private TestInstanceEventLog eventLog;

	// Session state shared between all runners of this testmodule instance, so that e.g. a login
	// performed by one runner is seen by the next (needed for the OIDC prompt=login tests).
	private CookieManager cookieManager = new CookieManager(); // HtmlUnit
	private volatile String playwrightStorageState; // Playwright: BrowserContext.storageState() JSON

	public BrowserControl(JsonObject config, String testId, TestInstanceEventLog eventLog, TestExecutionManager executionManager, ImageService imageService) {
		this.testId = testId;
		this.eventLog = eventLog;
		this.executionManager = executionManager;
		this.imageService = imageService;

		browserCommands = config.getAsJsonArray("browser");
		if (browserCommands == null) {
			browserCommands = new JsonArray();
		}
		this.verboseLogging = false;
		JsonElement browserVerbose = config.get("browser_verbose");
		if (browserVerbose != null) {
			this.verboseLogging = OIDFJSON.getBoolean(browserVerbose);
		}
		// CSS parsing was profiled at ~20% of CI CPU during the test stage; the
		// conformance suite tests OIDC protocols, not visual rendering, so a
		// test config can opt out by setting options.browsercontrol_css_enable=false.
		JsonObject options = config.getAsJsonObject("options");
		if (options != null) {
			JsonElement cssEnable = options.get("browsercontrol_css_enable");
			if (cssEnable != null) {
				this.cssParsingEnabled = OIDFJSON.getBoolean(cssEnable);
			}
		}

		this.engine = System.getProperty("browser.engine", ENGINE_SELENIUM).toLowerCase(Locale.ROOT);
		if (!engine.equals(ENGINE_SELENIUM) && !engine.equals(ENGINE_PLAYWRIGHT)) {
			throw new IllegalStateException("Unsupported browser.engine '" + engine + "', expected "
				+ ENGINE_SELENIUM + " or " + ENGINE_PLAYWRIGHT);
		}
	}

	/**
	 * The scripted browser engine in use, {@link #ENGINE_SELENIUM} or {@link #ENGINE_PLAYWRIGHT}.
	 */
	public String getEngine() {
		return engine;
	}

	private BrowserRunner createRunner(BrowserVisit visit) {
		if (engine.equals(ENGINE_PLAYWRIGHT)) {
			return new PlaywrightBrowserRunner(this, testId, eventLog, PlaywrightBrowserRunner.Settings.fromSystemProperties(), visit);
		}
		return new SeleniumBrowserRunner(this, testId, eventLog, cookieManager, verboseLogging, cssParsingEnabled, visit);
	}

	/**
	 * Tell the front-end control that a url needs to be visited. If there is a matching
	 * browser configuration element, this will execute automatically. If there is no
	 * matching element, the url is made available for user interaction.
	 *
	 * @param url the url to be visited
	 */
	public void goToUrl(String url) {
		goToUrl(url, null);
	}

	public void goToUrl(String url, String placeholder) {
		goToUrl(url, placeholder, "GET");
	}

	/**
	 * Tell the front-end control that a url needs to be visited. If there is a matching
	 * browser configuration element, this will execute automatically. If there is no
	 * matching element, the url is made available for user interaction.
	 *
	 * @param url         the url to be visited
	 * @param placeholder the placeholder in the log that is expecting the results of
	 *                    the transaction, usually as a screenshot, can be null
	 * @param method	  the HTTP method to be used
	 */
	public void goToUrl(String url, String placeholder, String method) {
		goToUrl(url, placeholder, method, 0);
	}

	/**
	 * Returns true if the test configuration contains a 'browser' automation entry matching the
	 * given url, i.e. goToUrl() would run the scripted browser for it instead of leaving the url
	 * for manual user interaction.
	 *
	 * @param url the url to check
	 */
	public boolean urlMatchesBrowserAutomation(String url) {
		for (JsonElement commandsEl : browserCommands) {
			JsonObject commands = commandsEl.getAsJsonObject();
			String urlMatcher = OIDFJSON.getString(commands.get("match"));
			if (PatternMatchUtils.simpleMatch(urlMatcher, url)) {
				if (commands.has("match-limit") && OIDFJSON.getInt(commands.get("match-limit")) <= 0) {
					continue;
				}
				return true;
			}
		}
		return false;
	}

	public void goToUrl(String url, String placeholder, String method, int delaySeconds){

			// find the first matching command set based on the url pattern in 'match'
		logger.debug(testId + ": goToUrl called for " + url);
		for (JsonElement commandsEl : browserCommands) {
			JsonObject commands = commandsEl.getAsJsonObject();
			String urlMatcher = OIDFJSON.getString(commands.get("match"));
			logger.debug(testId + ": Checking against URL MATCHER: " + urlMatcher);
			if (PatternMatchUtils.simpleMatch(urlMatcher, url)) {
				if (commands.has("match-limit")) {
					int limit = OIDFJSON.getInt(commands.get("match-limit"));
					logger.debug(testId + ": Current limit: " + limit);
					if (limit <= 0) {
						continue;
					}
					limit--;
					commands.addProperty("match-limit", limit);
				}
				BrowserRunner wr = createRunner(new BrowserVisit(url, commands.getAsJsonArray("tasks"), placeholder, method, delaySeconds));
				executionManager.runInBackground(wr);
				logger.debug(testId + ": " + engine + " BrowserRunner submitted to task executor for: " + url);

				runners.add(wr);

				return;
			}
		}
		logger.debug(testId + ": Could not find a match for url: " + url);
		if (verboseLogging) {
			eventLog.log("BROWSER", "asking user to visit url, no automation for found: " + url);
		}
		// if we couldn't find a command for this URL, leave it up to the user to do something with it
		urls.add(url);
		urlsWithMethod.add(new UrlWithMethod(url, method));
	}

	/**
	 * Request a credential using the Browser API
	 * @param request JSON object that will be passed to the browser API
	 * @param submitUrl URL that log-detail.html should send the results of the browser API call back to
	 */
	public void requestCredential(JsonObject request, String submitUrl) {
		browserApiRequests.add(new BrowserApiRequest(request, submitUrl));
	}

	/**
	 * Ask the user to paste a URI (e.g. an openid4vp:// authorization request) whose query
	 * string log-detail.html should submit to the given endpoint. Repeated calls with the
	 * same submitUrl are ignored so the box is only shown once.
	 *
	 * @param submitUrl   endpoint the pasted URI's query string is appended to and submitted to
	 * @param description text shown to the user explaining what to paste
	 */
	public void requestUriInput(String submitUrl, String description) {
		if (uriInputRequests.stream().anyMatch(r -> Objects.equals(submitUrl, r.getSubmitUrl()))) {
			return;
		}
		uriInputRequests.add(new UriInputRequest(submitUrl, description));
	}

	/**
	 * Tell the front end control that a url has been visited by the user externally.
	 *
	 * @param url the url that has been visited
	 */
	public void urlVisited(String url) {
		logger.info(testId + ": Browser went to: " + url);

		urls.remove(url);
		visited.add(url);

		Optional<UrlWithMethod> urlWithMethod = urlsWithMethod.stream().filter(u -> Objects.equals(url, u.getUrl())).findFirst();
		if (urlWithMethod.isPresent()) {
			urlsWithMethod.remove(urlWithMethod.get());
			visitedUrlsWithMethod.add(urlWithMethod.get());
		}
	}

	/**
	 * Called by a {@link BrowserRunner} when it has finished, successfully or not.
	 */
	void removeRunner(BrowserRunner runner) {
		runners.remove(runner);
	}

	/**
	 * Storage state (cookies, local storage) left behind by the last Playwright runner, or null.
	 */
	String getPlaywrightStorageState() {
		return playwrightStorageState;
	}

	void setPlaywrightStorageState(String storageState) {
		this.playwrightStorageState = storageState;
	}

	/**
	 * Get the list of URLs that require user interaction.
	 *
	 * @return
	 */
	public List<String> getUrls() {
		return urls;
	}

	public List<UrlWithMethod> getUrlsWithMethod() {
		return urlsWithMethod;
	}

	public List<BrowserApiRequest> getBrowserApiRequests() {
		return browserApiRequests;
	}

	public List<UriInputRequest> getUriInputRequests() {
		return uriInputRequests;
	}

	public List<UrlWithMethod> getVisitedUrlsWithMethod() {
		return visitedUrlsWithMethod;
	}

	public boolean showQrCodes() {
		return this.showQrCodes;
	}

	public void setShowQrCodes(boolean showQrCodes) {
		this.showQrCodes = showQrCodes;
	}

	/**
	 * Publish the given page content to fulfill the placeholder.
	 *
	 * @param placeholder         the placeholder to fulfill
	 * @param pageSource          the source of the page as rendered
	 * @param responseContentType the content type last received from the server
	 */
	void updatePlaceholder(String placeholder, String pageSource, String responseContentType, String regexp, boolean optional) {
		Map<String, Object> update = new HashMap<>();
		update.put("page_source", pageSource);
		update.put("content_type", responseContentType);
		update.put("matched_regexp", regexp);

		Document document = imageService.fillPlaceholder(testId, placeholder, update, true);
		if (document == null) {
			if (optional) {
				eventLog.log("BROWSER", args("msg", "Skipping optional placeholder update as placeholder not found.", "placeholder", placeholder));
				return;
			}
			throw new TestFailureException(testId, "Couldn't find matched placeholder for uploading error screenshot.");
		}

		eventLog.log("BROWSER", args("msg", "Updated placeholder from scripted browser", "placeholder", placeholder));

		if (imageService.getRemainingPlaceholders(testId, true).isEmpty()) {
			// no remaining placeholders
			eventLog.log("BROWSER", args("msg", "All placeholders filled by scripted browser"));
		}
	}

	/**
	 * Get the list of URLs that have been visited.
	 *
	 * @return
	 */
	public List<String> getVisited() {
		return visited;
	}

	/**
	 * Get the properties of any currently running webrunners.
	 *
	 * @return
	 */
	public List<JsonObject> getWebRunners() {
		List<JsonObject> out = new ArrayList<>();

		for (BrowserRunner runner : runners) {
			out.add(runner.getStatus());
		}

		return out;
	}

	public boolean runnersActive() {
		return !runners.isEmpty();
	}
}
