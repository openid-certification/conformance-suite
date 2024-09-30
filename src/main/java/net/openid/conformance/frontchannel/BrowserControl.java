package net.openid.conformance.frontchannel;

import com.google.common.base.Strings;
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
import org.htmlunit.HttpMethod;
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
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.PatternMatchUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

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
	private Queue<WebRunner> runners = new ConcurrentLinkedQueue<>();

	private ImageService imageService;

	private TestInstanceEventLog eventLog;

	private CookieManager cookieManager = new CookieManager(); // cookie manager, shared between all webrunners for this testmodule instance

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
				WebRunner wr = new WebRunner(url, commands.getAsJsonArray("tasks"), placeholder, method);
				executionManager.runInBackground(wr);
				logger.debug(testId + ": WebRunner submitted to task executor for: " + url);

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
	 * Private Runnable class that acts as the browser and allows goToUrl to return before the page gets hit.
	 * This gets handed to a {@link TaskExecutor} which manages the thread it gets run on
	 */
	private class WebRunner implements Callable<String> {
		private String url;
		private ResponseCodeHtmlUnitDriver driver;
		private JsonArray tasks;
		private String currentTask;
		private String currentCommand;
		private String lastException;
		private String placeholder;
		private String method;

		/**
		 * @param url   url to go to
		 * @param tasks {@link JsonArray} of commands to perform once we get to the page
		 */
		private WebRunner(String url, JsonArray tasks, String placeholder, String method) {
			this.url = url;
			this.tasks = tasks;
			this.placeholder = placeholder;
			this.method = method;

			// each WebRunner gets it's own driver... that way two could run at the same time for the same test.
			this.driver = new ResponseCodeHtmlUnitDriver();
		}

		@Override
		public String call() {
			try {
				logger.info(testId + ": Sending BrowserControl to: " + url);

				if (Objects.equals(method, "POST")) {

					URL urlWithQueryString = new URL(url);
					URL urlWithoutQuery = new URL(urlWithQueryString.getProtocol(), urlWithQueryString.getHost(), urlWithQueryString.getPort(), urlWithQueryString.getPath());
					String params = urlWithQueryString.getQuery();
					WebClient client = driver.getWebClient();
					WebRequest request = new WebRequest(urlWithoutQuery, HttpMethod.POST);
					request.setAdditionalHeader("Content-Type", "application/x-www-form-urlencoded");
					request.setRequestBody(params);

					eventLog.log("WebRunner", args(
						"msg", "Scripted browser HTTP request",
						"http", "request",
						"request_uri", urlWithoutQuery.toString(),
						"parameters", params,
						"request_method", method,
						"browser", "goToUrl"
					));

					// do the actual HTTP POST
					client.getPage(request);

				} else {

					eventLog.log("WebRunner", args(
						"msg", "Scripted browser HTTP request",
						"http", "request",
						"request_uri", url,
						"request_method", method,
						"browser", "goToUrl"
					));

					// do the actual HTTP GET
					driver.get(url);

				}

				eventLog.log("WebRunner", args(
					"msg", "Scripted browser HTTP response",
					"http", "response",
					"response_status_code", driver.getResponseCode(),
					"response_status_text", driver.getStatus(),
					"response_content_type", driver.getResponseContentType(),
					"response_content", driver.getResponseContent()
				));


				int responseCode = driver.getResponseCode();

				for (int i = 0; i < this.tasks.size(); i++) {
					boolean skip = false;

					JsonObject currentTask = this.tasks.get(i).getAsJsonObject();

					if (currentTask.get("task") == null) {
						throw new TestFailureException(testId, "Invalid Task Definition: no 'task' property");
					}

					String taskName = OIDFJSON.getString(currentTask.get("task"));

					this.currentTask = taskName;

					logger.debug(testId + ": Performing: " + taskName);
					logger.debug(testId + ": WebRunner current url:" + driver.getCurrentUrl());
					// check if current URL matches the 'matcher' for the task

					String expectedUrlMatcher = "*"; // default to matching any URL
					if (currentTask.has("match")) {
						// if there is a more specific "match" element, use its value instead
						expectedUrlMatcher = OIDFJSON.getString(currentTask.get("match"));
					}

					if (!Strings.isNullOrEmpty(expectedUrlMatcher)) {
						if (!PatternMatchUtils.simpleMatch(expectedUrlMatcher, driver.getCurrentUrl())) {
							if (currentTask.has("optional") && OIDFJSON.getBoolean(currentTask.get("optional"))) {
								eventLog.log("WebRunner", args(
									"msg", "Skipping optional task due to URL mismatch",
									"match", expectedUrlMatcher,
									"url", driver.getCurrentUrl(),
									"browser", "skip",
									"task", taskName,
									"commands", currentTask.get("commands")
								));

								skip = true; // we're going to skip this command
							} else {
								eventLog.log("WebRunner", args(
									"msg", "Unexpected URL for non-optional task",
									"match", expectedUrlMatcher,
									"url", driver.getCurrentUrl(),
									"result", Condition.ConditionResult.FAILURE,
									"task", taskName,
									"commands", currentTask.get("commands")
								));

								throw new TestFailureException(testId, "WebRunner unexpected url for task: " + OIDFJSON.getString(currentTask.get("task")));
							}
						}

					}

					// if it does run the commands
					if (!skip) {
						JsonArray commands = currentTask.getAsJsonArray("commands");
						if (commands != null) { // we can have zero commands to just do a check that currentUrl is what we expect

							// wait for webpage to finish loading
							WebDriverWait waiting = new WebDriverWait(driver, Duration.ofSeconds(10), Duration.ofMillis(100));
							try {
								waiting.until((ExpectedCondition<Boolean>) webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
							} catch (TimeoutException timeoutException) {
								logger.error(testId + ": WebRunner caught exception: ", timeoutException);
								eventLog.log("BROWSER", ex(timeoutException, Map.of("msg", "Timeout waiting for page to load")));
							}

							// execute all of the commands in this task
							for (int j = 0; j < commands.size(); j++) {
								doCommand(commands.get(j).getAsJsonArray(), taskName);
								// clear the current command once it's done
								this.currentCommand = null;
							}
						}

						// Check the server response (Completing all browser command tasks should result in a submit/new page.)

						responseCode = driver.getResponseCode();
						logger.debug(testId + ":     Response Code: " + responseCode);

						eventLog.log("WebRunner", args(
							"msg", "Completed processing of webpage",
							"match", expectedUrlMatcher,
							"url", driver.getCurrentUrl(),
							"browser", "complete",
							"task", taskName,
							"result", Condition.ConditionResult.INFO,
							"response_status_code", driver.getResponseCode(),
							"response_status_text", driver.getStatus()
						));
					} // if we don't run the commands, just go straight to the next one
				}
				logger.debug(testId + ": Completed Browser Commands");
				// if we've successfully completed the command set, consider this URL visited
				urlVisited(url);

				return "web runner exited";
			} catch (Exception | Error e) {
				logger.error(testId + ": WebRunner caught exception", e);
				eventLog.log("WebRunner",
					ex(e,
						args("msg", e.getMessage(),
							"page_source", driver.getPageSource(),
							"url", driver.getCurrentUrl(),
							"content_type", driver.getResponseContentType(),
							"result", Condition.ConditionResult.FAILURE,
							"current_dom", driver.getCurrentDomAsXml())));
				this.lastException = e.getMessage();
				if (e instanceof TestFailureException) {
					// avoid wrapping a TestFailureException around a TestFailureException
					throw new TestFailureException(testId, "Web Runner Exception: " + e.getMessage(), e.getCause());
				}
				throw new TestFailureException(testId, "Web Runner Exception: " + e.getMessage(), e);
			} finally {
				runners.remove(this);
				driver.close();
			}
		}

		/**
		 * Given a command like '["click","id","btnId"], this will perform the WebDriver calls to execute it.
		 * Only two action types are supported this way: "click" to click on a WebElement, and "text" which enters
		 * text into a field like an input box.
		 *
		 * @param command
		 * @throws TestFailureException if an invalid command is specified
		 */
		private void doCommand(JsonArray command, String taskName) {
			// general format for command is [command_string, element_id_type, element_id, other_args]
			String commandString = OIDFJSON.getString(command.get(0));
			if (!Strings.isNullOrEmpty(commandString)) {

				this.currentCommand = commandString;

				// selectors common to all elements
				String elementType = OIDFJSON.getString(command.get(1));
				String target = OIDFJSON.getString(command.get(2));

				if (commandString.equalsIgnoreCase("click")) {
					// ["click", "id" or "name", "id_or_name"]

					eventLog.log("WebRunner", args(
						"msg", "Clicking an element",
						"url", driver.getCurrentUrl(),
						"browser", commandString,
						"task", taskName,
						"element_type", elementType,
						"target", target,
						"result", Condition.ConditionResult.INFO
					));

					try {
						driver.findElement(getSelector(elementType, target)).click();
					} catch (NoSuchElementException e) {
						String optional = command.size() >= 4 ? OIDFJSON.getString(command.get(3)) : null;
						if (optional != null && optional.equals("optional")) {
							eventLog.log("WebRunner", args(
								"msg", "Element not found, skipping as 'click' command is marked 'optional'",
								"url", driver.getCurrentUrl(),
								"browser", commandString,
								"task", taskName,
								"element_type", elementType,
								"target", target,
								"result", Condition.ConditionResult.INFO
							));
						} else {
							throw e;
						}
					}

					logger.debug(testId + ": Clicked: " + target + " (" + elementType + ")");
				} else if (commandString.equalsIgnoreCase("text")) {
					// ["text", "id" or "name", "id_or_name", "text_to_enter", "optional"]

					String value = OIDFJSON.getString(command.get(3));

					eventLog.log("WebRunner", args(
						"msg", "Entering text",
						"url", driver.getCurrentUrl(),
						"browser", commandString,
						"task", taskName,
						"element_type", elementType,
						"target", target,
						"value", value,
						"result", Condition.ConditionResult.INFO
					));

					try {
						WebElement entryBox = driver.findElement(getSelector(elementType, target));

						entryBox.clear();
						entryBox.sendKeys(value);
						logger.debug(testId + ":\t\tEntered text: '" + value + "' into " + target + " (" + elementType + ")");
					} catch (NoSuchElementException e) {
						String optional = command.size() >= 5 ? OIDFJSON.getString(command.get(4)) : null;
						if (optional != null && optional.equals("optional")) {
							eventLog.log("WebRunner", args(
								"msg", "Element not found, skipping as 'text' command is marked 'optional'",
								"url", driver.getCurrentUrl(),
								"browser", commandString,
								"task", taskName,
								"element_type", elementType,
								"target", target,
								"value", value,
								"result", Condition.ConditionResult.INFO
							));
						} else {
							throw e;
						}
					}

				} else if (commandString.equalsIgnoreCase("wait")) {
					// ["wait","match" or "contains", "urlmatch_or_contains_string",timeout_in_seconds]
					// 	 'wait' will wait for the URL to match a regex, or for it to contain a string, OR
					//	 'wait' can wait for the presence of an element (like a button) using the same selectors (id, name) as click and text above.
					// if waiting for an element, the next parameter can be a regexp to be matched
					// and the final parameter can be 'update-image-placeholder' to mark an image placeholder as satisfied

					int timeoutSeconds = OIDFJSON.getInt(command.get(3));
					String regexp = command.size() >= 5 ? OIDFJSON.getString(command.get(4)) : null;
					String action = command.size() >= 6 ? OIDFJSON.getString(command.get(5)) : null;
					boolean updateImagePlaceHolder = false;
					boolean updateImagePlaceHolderOptional = false;
					if (!Strings.isNullOrEmpty(action)) {
						if (action.equals("update-image-placeholder-optional")) {
							updateImagePlaceHolderOptional = true;
						} else if (action.equals("update-image-placeholder")) {
							updateImagePlaceHolder = true;
						} else {
							this.lastException = "Invalid action: " + action;
							throw new TestFailureException(testId, "Invalid action: " + action);
						}
					}

					eventLog.log("WebRunner", args(
						"msg", "Waiting",
						"url", driver.getCurrentUrl(),
						"browser", commandString,
						"task", taskName,
						"element_type", elementType,
						"target", target,
						"seconds", timeoutSeconds,
						"result", Condition.ConditionResult.INFO,
						"regexp", regexp,
						"action", action
					));
					// hook to wait for this condition, check every 100 milliseconds until the max seconds
					WebDriverWait waiting = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds), Duration.ofMillis(100));
					try {
						if (elementType.equalsIgnoreCase("contains")) {
							waiting.until(ExpectedConditions.urlContains(target));
						} else if (elementType.equalsIgnoreCase("match")) {
							waiting.until(ExpectedConditions.urlMatches(target)); // NB this takes a regexp
						} else if (!Strings.isNullOrEmpty(regexp)) {
							Pattern pattern = Pattern.compile(regexp);
							waiting.until(ExpectedConditions.textMatches(getSelector(elementType, target), pattern));
							if (updateImagePlaceHolder || updateImagePlaceHolderOptional) {
								// make a snapshot of the page available to the test log
								updatePlaceholder(this.placeholder, driver.getPageSource(), driver.getResponseContentType(), regexp, updateImagePlaceHolderOptional);
							}
						} else {
							waiting.until(ExpectedConditions.presenceOfElementLocated(getSelector(elementType, target)));
						}

						logger.debug(testId + ":\t\tDone waiting: " + commandString);

					} catch (TimeoutException timeoutException) {
						this.lastException = timeoutException.getMessage();
						throw new TestFailureException(testId, "Timed out waiting: " + command.toString());
					}
				} else if (commandString.equalsIgnoreCase("wait-element-invisible")) {
					int timeoutSeconds = OIDFJSON.getInt(command.get(3));
					try {
						WebDriverWait waiting = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds), Duration.ofMillis(100));
						waiting.until(ExpectedConditions.invisibilityOfElementLocated(getSelector(elementType, target)));
						logger.debug(testId + ":\t\tElement with " + elementType + " '" + target + "' is now invisible");
					} catch (TimeoutException timeoutException) {
						this.lastException = timeoutException.getMessage();
						throw new TestFailureException(testId, "Timed out waiting for element to become invisible: " + command.toString());
					}
				} else if (commandString.equalsIgnoreCase("wait-element-visible")) {
					int timeoutSeconds = OIDFJSON.getInt(command.get(3));
					try {
						WebDriverWait waiting = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds), Duration.ofMillis(100));
						waiting.until(ExpectedConditions.visibilityOfElementLocated(getSelector(elementType, target)));
						logger.debug(testId + ":\t\tElement with " + elementType + " '" + target + "' is now visible");
					} catch (TimeoutException timeoutException) {
						this.lastException = timeoutException.getMessage();
						throw new TestFailureException(testId, "Timed out waiting for element visibility: " + command.toString());
					}
				} else {
					this.lastException = "Invalid Command " + commandString;
					throw new TestFailureException(testId, "Invalid Command: " + commandString);
				}
			} else {
				// can't have a blank command
				this.lastException = "Invalid Command " + commandString;
				throw new TestFailureException(testId, "Invalid Command: " + commandString);
			}
		}

		/**
		 * Returns the appropriate {@link By} statement based on type and value.
		 * Currently, supports id, name, xpath, css (css selector), and class (html class)
		 *
		 * @param type
		 * @param value
		 * @return
		 * @throws TestFailureException if an invalid type is specified.
		 */
		private By getSelector(String type, String value) {
			if (type.equalsIgnoreCase("id")) {
				return By.id(value);
			} else if (type.equalsIgnoreCase("name")) {
				return By.name(value);
			} else if (type.equalsIgnoreCase("xpath")) {
				return By.xpath(value);
			} else if (type.equalsIgnoreCase("css")) {
				return By.cssSelector(value);
			} else if (type.equalsIgnoreCase("class")) {
				return By.className(value);
			}
			this.lastException = "Invalid Command Selector: Type: " + type + " Value: " + value;
			throw new TestFailureException(testId, "Invalid Command Selector: Type: " + type + " Value: " + value);
		}

	}

	// Allow access to the response code via the HtmlUnit instance. The driver doesn't normally have this functionality.

	@SuppressWarnings("serial")
	private static class BrowserControlPageCreator extends DefaultPageCreator {
		// this is necessary because:
		// curl -v 'https://fapidev-as.authlete.net/api/authorization?client_id=21541757519&redirect_uri=https://localhost:8443/test/a/authlete-fapi/callback&scope=openid%20accounts&state=ND4WAuQ8lt&nonce=lOgNDes2YE&response_type=code%20id_token'
		// returns:
		// Content-Type: */*;charset=utf-8
		// so we need to override this so it's treated as html, which is how browsers treat it
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
				"result", Condition.ConditionResult.INFO
			));

			WebResponse response = super.getResponse(webRequest);

			if (response.getStatusCode() == 302) {
				eventLog.log("WebRunner", args(
					"msg", "Redirect " + response.getStatusCode() + " " + response.getStatusMessage() + " to " + response.getResponseHeaderValue("location") + " from " + webRequest.getHttpMethod() + " " + webRequest.getUrl(),
					"headers", mapHeadersToJsonObject(response.getResponseHeaders()),
					"body", response.getContentAsString(),
					"result", Condition.ConditionResult.INFO
				));
			} else {
				eventLog.log("WebRunner", args(
					"msg", "Response " + response.getStatusCode() + " " + response.getStatusMessage() + " " + webRequest.getHttpMethod() + " " + webRequest.getUrl(),
					"headers", mapHeadersToJsonObject(response.getResponseHeaders()),
					"body", response.getContentAsString(),
					"result", Condition.ConditionResult.INFO
				));
			}

			return response;
		}
	}

	/**
	 * SubClass of {@link HtmlUnitDriver} to provide access to the response code of the last page we visited
	 */
	private class ResponseCodeHtmlUnitDriver extends HtmlUnitDriver {

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
			// This is necessary for OIDC prompt=login tests. It might make the results unpredictable if we are running
			// multiple WebRunners within one test module instance at the same time, as the ordering of when cookies
			// are set/read might differ between test runs.
			client.setCookieManager(cookieManager);

			// Selenium / HtmlUnit's javascript engine barfs at a lot of modern
			// javascript. However asking it to ignore the errors and carry on seems
			// to result in a surprising amount of eventual success.
			client.getOptions().setThrowExceptionOnScriptError(false);

			client.setJavaScriptErrorListener(new JavaScriptErrorListener() {

				@Override
				public void scriptException(HtmlPage page, ScriptException scriptException) {
					eventLog.log("BROWSER", args("msg", "Error during JavaScript execution", "detail", scriptException.toString()));
				}

				@Override
				public void timeoutError(HtmlPage page, long allowedTime, long executionTime) {
					eventLog.log("BROWSER", args("msg", "Timeout during JavaScript execution after "
						+ executionTime + "ms; allowed only " + allowedTime + "ms"));

				}

				@Override
				public void malformedScriptURL(HtmlPage page, String url, MalformedURLException malformedURLException) {
					eventLog.log("BROWSER", args("msg", "Unable to build URL for script src tag [" + url + "]", "exception", malformedURLException.toString()));
				}

				@Override
				public void loadScriptError(HtmlPage page, URL scriptUrl, Exception exception) {
					eventLog.log("BROWSER", args("msg", "Error loading JavaScript from [" + scriptUrl + "].", "exception", exception.toString()));
				}

				@Override
				public void warn(String message, String sourceName, int line, String lineSource, int lineOffset) {
					final StringBuilder msg = new StringBuilder()
						.append("warning: message=[").append(message)
						.append("] sourceName=[").append(sourceName)
						.append("] line=[").append(line)
						.append("] lineSource=[").append(lineSource)
						.append("] lineOffset=[").append(lineOffset)
						.append("]");

					eventLog.log("BROWSER", args("msg", msg.toString()));
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
	private void updatePlaceholder(String placeholder, String pageSource, String responseContentType, String regexp, boolean optional) {
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

		for (WebRunner wr : runners) {
			JsonObject o = new JsonObject();
			o.addProperty("url", wr.url);
			o.addProperty("currentUrl", wr.driver.getCurrentUrl());
			o.addProperty("currentTask", wr.currentTask);
			o.addProperty("currentCommand", wr.currentCommand);
			o.addProperty("lastResponseCode", wr.driver.getResponseCode());
			o.addProperty("lastResponseContentType", wr.driver.getResponseContentType());
			o.addProperty("lastResponseContent", wr.driver.getResponseContent());
			o.addProperty("lastException", wr.lastException);

			out.add(o);
		}

		return out;
	}

	public boolean runnersActive() {
		return !runners.isEmpty();
	}
}
