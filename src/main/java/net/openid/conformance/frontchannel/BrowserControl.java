package net.openid.conformance.frontchannel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.info.ImageService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.runner.TestExecutionManager;
import net.openid.conformance.testmodule.DataUtils;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import org.bson.Document;
import org.htmlunit.BrowserVersion;
import org.htmlunit.CookieManager;
import org.htmlunit.DefaultPageCreator;
import org.htmlunit.HttpWebConnection;
import org.htmlunit.Page;
import org.htmlunit.ScriptException;
import org.htmlunit.WebClient;
import org.htmlunit.WebConsole;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.WebWindow;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.javascript.JavaScriptErrorListener;
import org.htmlunit.util.NameValuePair;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.PatternMatchUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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

	private String testId;

	private TestExecutionManager executionManager;
	private JsonArray browserCommands = null;
	private boolean verboseLogging;
	private boolean showQrCodes = false;

	private List<String> urls = new ArrayList<>();
	private List<UrlWithMethod> urlsWithMethod = new ArrayList<>();
	private List<String> visited = new ArrayList<>();
	private List<UrlWithMethod> visitedUrlsWithMethod = new ArrayList<>();
	private List<BrowserApiRequest> browserApiRequests = new ArrayList<>();
	private Queue<IBrowserRunner> runners = new ConcurrentLinkedQueue<>();

	private ImageService imageService;

	private TestInstanceEventLog eventLog;

	private CookieManager cookieManager = new CookieManager(); // cookie manager, shared between all webrunners for this
																// testmodule instance

	// Browser engine configuration
	private String browserEngine;
	private String playwrightBrowserType;
	private boolean playwrightHeadless;
	private int playwrightSlowMo;

	public BrowserControl(JsonObject config, String testId, TestInstanceEventLog eventLog,
			TestExecutionManager executionManager, ImageService imageService) {
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

		// Read browser engine configuration from system properties
		this.browserEngine = System.getProperty("fintechlabs.browser.engine", "selenium");
		this.playwrightBrowserType = System.getProperty("fintechlabs.browser.playwright.type", "chromium");
		this.playwrightHeadless = Boolean
				.parseBoolean(System.getProperty("fintechlabs.browser.playwright.headless", "true"));
		this.playwrightSlowMo = Integer.parseInt(System.getProperty("fintechlabs.browser.playwright.slowMo", "0"));

		logger.info("Browser automation engine: " + this.browserEngine);
		if ("playwright".equals(this.browserEngine)) {
			logger.info("Playwright browser type: " + this.playwrightBrowserType + ", headless: "
					+ this.playwrightHeadless + ", slowMo: " + this.playwrightSlowMo + "ms");
		}
	}

	/**
	 * Factory method to create appropriate browser runner based on configuration
	 */
	private IBrowserRunner createBrowserRunner(String url, JsonArray tasks, String placeholder,
			String method, int delaySeconds) {
		if ("playwright".equals(browserEngine)) {
			return new PlaywrightBrowserRunner(url, tasks, placeholder, method, delaySeconds,
					testId, eventLog, this, cookieManager,
					playwrightHeadless, playwrightBrowserType, playwrightSlowMo);
		} else {
			// Default to Selenium
			return new SeleniumBrowserRunner(url, tasks, placeholder, method, delaySeconds,
					testId, eventLog, this, cookieManager);
		}
	}

	/**
	 * Tell the front-end control that a url needs to be visited. If there is a
	 * matching
	 * browser configuration element, this will execute automatically. If there is
	 * no
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
	 * Tell the front-end control that a url needs to be visited. If there is a
	 * matching
	 * browser configuration element, this will execute automatically. If there is
	 * no
	 * matching element, the url is made available for user interaction.
	 *
	 * @param url         the url to be visited
	 * @param placeholder the placeholder in the log that is expecting the results
	 *                    of
	 *                    the transaction, usually as a screenshot, can be null
	 * @param method      the HTTP method to be used
	 */
	public void goToUrl(String url, String placeholder, String method) {
		goToUrl(url, placeholder, method, 0);
	}

	public void goToUrl(String url, String placeholder, String method, int delaySeconds) {

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
				IBrowserRunner wr = createBrowserRunner(url, commands.getAsJsonArray("tasks"), placeholder, method,
						delaySeconds);
				executionManager.runInBackground(wr);
				logger.debug(testId + ": " + browserEngine + " BrowserRunner submitted to task executor for: " + url);

				runners.add(wr);

				return;
			}
		}
		logger.debug(testId + ": Could not find a match for url: " + url);
		if (verboseLogging) {
			eventLog.log("BROWSER", "asking user to visit url, no automation for found: " + url);
		}
		// if we couldn't find a command for this URL, leave it up to the user to do
		// something with it
		urls.add(url);
		urlsWithMethod.add(new UrlWithMethod(url, method));
	}

	/**
	 * Request a credential using the Browser API
	 *
	 * @param request   JSON object that will be passed to the browser API
	 * @param submitUrl URL that log-detail.html should send the results of the
	 *                  browser API call back to
	 */
	public void requestCredential(JsonObject request, String submitUrl) {
		browserApiRequests.add(new BrowserApiRequest(request, submitUrl));
	}

	/**
	 * Tell the front end control that a url has been visited by the user
	 * externally.
	 *
	 * @param url the url that has been visited
	 */
	public void urlVisited(String url) {
		logger.info(testId + ": Browser went to: " + url);

		urls.remove(url);
		visited.add(url);

		Optional<UrlWithMethod> urlWithMethod = urlsWithMethod.stream().filter(u -> Objects.equals(url, u.getUrl()))
				.findFirst();
		if (urlWithMethod.isPresent()) {
			urlsWithMethod.remove(urlWithMethod.get());
			visitedUrlsWithMethod.add(urlWithMethod.get());
		}
	}

	/**
	 * Remove a runner from the active runners queue.
	 * Called by runners when they complete execution.
	 *
	 * @param runner The runner to remove
	 */
	void removeRunner(IBrowserRunner runner) {
		runners.remove(runner);
	}

	@SuppressWarnings("serial")
	private static class BrowserControlPageCreator extends DefaultPageCreator {
		// this is necessary because:
		// curl -v
		// 'https://fapidev-as.authlete.net/api/authorization?client_id=21541757519&redirect_uri=https://localhost:8443/test/a/authlete-fapi/callback&scope=openid%20accounts&state=ND4WAuQ8lt&nonce=lOgNDes2YE&response_type=code%20id_token'
		// returns:
		// Content-Type: */*;charset=utf-8
		// so we need to override this so it's treated as html, which is how browsers
		// treat it
		@Override
		public Page createPage(final WebResponse webResponse, final WebWindow webWindow) throws IOException {
			return createHtmlPage(webResponse, webWindow);
		}
	}

	private class LoggingHttpWebConnection extends HttpWebConnection {

		public LoggingHttpWebConnection(WebClient webClient) {
			super(webClient);
		}

		/**
		 * Convert headers returned by HTMLUnit into a JsonObject
		 */
		public JsonObject mapHeadersToJsonObject(List<NameValuePair> headers) {
			JsonObject o = new JsonObject();
			for (NameValuePair pair : headers) {
				String name = pair.getName();
				if (o.has(name)) {
					// If header occurs multiple times, put each value as an array element
					JsonArray array;
					JsonElement existing = o.get(name);
					if (existing.isJsonPrimitive()) {
						array = new JsonArray();
					} else {
						array = (JsonArray) existing;
					}
					array.add(pair.getValue());
				} else {
					o.addProperty(name, pair.getValue());
				}
			}
			return o;
		}

		@Override
		public WebResponse getResponse(WebRequest webRequest) throws IOException {
			eventLog.log("WebRunner", args(
					"msg", "Request " + webRequest.getHttpMethod() + " " + webRequest.getUrl(),
					"headers", webRequest.getAdditionalHeaders(),
					"params", webRequest.getRequestParameters(),
					"body", webRequest.getRequestBody(),
					"result", Condition.ConditionResult.INFO));

			WebResponse response = super.getResponse(webRequest);

			if (response.getStatusCode() == 302) {
				eventLog.log("WebRunner", args(
						"msg",
						"Redirect " + response.getStatusCode() + " " + response.getStatusMessage() + " to "
								+ response.getResponseHeaderValue("location") + " from " + webRequest.getHttpMethod()
								+ " " + webRequest.getUrl(),
						"headers", mapHeadersToJsonObject(response.getResponseHeaders()),
						"body", response.getContentAsString(),
						"result", Condition.ConditionResult.INFO));
			} else {
				eventLog.log("WebRunner", args(
						"msg",
						"Response " + response.getStatusCode() + " " + response.getStatusMessage() + " "
								+ webRequest.getHttpMethod() + " " + webRequest.getUrl(),
						"headers", mapHeadersToJsonObject(response.getResponseHeaders()),
						"body", response.getContentAsString(),
						"result", Condition.ConditionResult.INFO));
			}

			return response;
		}
	}

	/**
	 * SubClass of {@link HtmlUnitDriver} to provide access to the response code of
	 * the last page we visited
	 */
	class ResponseCodeHtmlUnitDriver extends HtmlUnitDriver {

		public ResponseCodeHtmlUnitDriver() {
			super(true);
			final WebConsole console = getWebClient().getWebConsole();
			console.setLogger(new WebConsole.Logger() {
				private void internalLog(final Object message) {
					if (verboseLogging) {
						eventLog.log("BROWSER", String.valueOf(message));
					}
					logger.info(String.valueOf(message));
				}

				@Override
				public void warn(final Object message) {
					internalLog(message);
				}

				@Override
				public boolean isErrorEnabled() {
					return true;
				}

				@Override
				public boolean isTraceEnabled() {
					return true;
				}

				@Override
				public void trace(final Object message) {
					internalLog(message);
				}

				@Override
				public boolean isDebugEnabled() {
					return true;
				}

				@Override
				public void info(final Object message) {
					internalLog(message);
				}

				@Override
				public boolean isWarnEnabled() {
					return true;
				}

				@Override
				public void error(final Object message) {
					internalLog(message);
				}

				@Override
				public void debug(final Object message) {
					internalLog(message);
				}

				@Override
				public boolean isInfoEnabled() {
					return true;
				}
			});

		}

		public int getResponseCode() {
			return this.getCurrentWindow().lastPage().getWebResponse().getStatusCode();
		}

		public String getResponseContent() {
			return this.getCurrentWindow().lastPage().getWebResponse().getContentAsString();
		}

		public String getResponseContentType() {
			return this.getCurrentWindow().lastPage().getWebResponse().getContentType();
		}

		public String getCurrentDomAsXml() {
			HtmlPage page = (HtmlPage) this.getCurrentWindow().lastPage();
			return page.getDocumentElement().asXml();
		}

		public String getStatus() {
			String responseCodeString = this.getCurrentWindow().lastPage().getWebResponse().getStatusCode() + "-" +
					this.getCurrentWindow().lastPage().getWebResponse().getStatusMessage();
			return responseCodeString;
		}

		@Override
		protected WebClient newWebClient(BrowserVersion version) {
			return new WebClient(version);
		}

		@Override
		protected WebClient modifyWebClient(WebClient client) {
			client.setPageCreator(new BrowserControlPageCreator());
			// use same cookie manager for all instances within this testmodule instance
			// (cookie manager seems to be thread safe)
			// This is necessary for OIDC prompt=login tests. It might make the results
			// unpredictable if we are running
			// multiple WebRunners within one test module instance at the same time, as the
			// ordering of when cookies
			// are set/read might differ between test runs.
			client.setCookieManager(cookieManager);

			// Selenium / HtmlUnit's javascript engine barfs at a lot of modern
			// javascript. However asking it to ignore the errors and carry on seems
			// to result in a surprising amount of eventual success.
			client.getOptions().setThrowExceptionOnScriptError(false);

			client.setJavaScriptErrorListener(new JavaScriptErrorListener() {

				@Override
				public void scriptException(HtmlPage page, ScriptException scriptException) {
					eventLog.log("BROWSER",
							args("msg", "Error during JavaScript execution", "detail", scriptException.toString()));
				}

				@Override
				public void timeoutError(HtmlPage page, long allowedTime, long executionTime) {
					eventLog.log("BROWSER", args("msg", "Timeout during JavaScript execution after "
							+ executionTime + "ms; allowed only " + allowedTime + "ms"));

				}

				@Override
				public void malformedScriptURL(HtmlPage page, String url, MalformedURLException malformedURLException) {
					eventLog.log("BROWSER", args("msg", "Unable to build URL for script src tag [" + url + "]",
							"exception", malformedURLException.toString()));
				}

				@Override
				public void loadScriptError(HtmlPage page, URL scriptUrl, Exception exception) {
					eventLog.log("BROWSER", args("msg", "Error loading JavaScript from [" + scriptUrl + "].",
							"exception", exception.toString()));
				}

				@Override
				public void warn(String message, String sourceName, int line, String lineSource, int lineOffset) {
					String msg = "warning: message=[" + message +
							"] sourceName=[" + sourceName +
							"] line=[" + line +
							"] lineSource=[" + lineSource +
							"] lineOffset=[" + lineOffset +
							"]";

					eventLog.log("BROWSER", args("msg", msg));
				}
			});

			if (verboseLogging) {
				client.setWebConnection(new LoggingHttpWebConnection(client));
			}

			return client;
		}
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
	void updatePlaceholder(String placeholder, String pageSource, String responseContentType, String regexp,
			boolean optional) {
		Map<String, Object> update = new HashMap<>();
		update.put("page_source", pageSource);
		update.put("content_type", responseContentType);
		update.put("matched_regexp", regexp);

		Document document = imageService.fillPlaceholder(testId, placeholder, update, true);
		if (document == null) {
			if (optional) {
				eventLog.log("BROWSER", args("msg", "Skipping optional placeholder update as placeholder not found.",
						"placeholder", placeholder));
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

		for (IBrowserRunner runner : runners) {
			JsonObject o = new JsonObject();

			// Support for SeleniumBrowserRunner
			if (runner instanceof SeleniumBrowserRunner) {
				SeleniumBrowserRunner wr = (SeleniumBrowserRunner) runner;
				o.addProperty("url", wr.getUrl());
				o.addProperty("currentUrl", wr.getCurrentUrl());
				o.addProperty("currentTask", wr.getCurrentTask());
				o.addProperty("currentCommand", wr.getCurrentCommand());
				o.addProperty("lastResponseCode", wr.getResponseCode());
				o.addProperty("lastResponseContentType", wr.getResponseContentType());
				o.addProperty("lastResponseContent", wr.getResponseContent());
				o.addProperty("lastException", wr.getLastException());
			} else if (runner instanceof PlaywrightBrowserRunner) {
				// Support for PlaywrightBrowserRunner
				PlaywrightBrowserRunner pr = (PlaywrightBrowserRunner) runner;
				o.addProperty("url", pr.getUrl());
				o.addProperty("currentUrl", pr.getCurrentUrl());
				o.addProperty("currentTask", pr.getCurrentTask());
				o.addProperty("currentCommand", pr.getCurrentCommand());
				o.addProperty("lastException", pr.getLastException());
			} else {
				// Generic fallback for other implementations
				o.addProperty("currentTask", runner.getCurrentTask());
				o.addProperty("currentCommand", runner.getCurrentCommand());
				o.addProperty("lastException", runner.getLastException());
			}

			out.add(o);
		}

		return out;
	}

	public boolean runnersActive() {
		return !runners.isEmpty();
	}
}
