package net.openid.conformance.frontchannel;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.DataUtils;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import org.htmlunit.CookieManager;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.PatternMatchUtils;

import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Selenium/HtmlUnit-based browser automation runner.
 * This is the original implementation using HtmlUnitDriver for browser
 * automation.
 *
 * Executes browser commands from JSON configuration including:
 * - Navigation (GET/POST)
 * - Element interaction (click, text input)
 * - Waiting conditions (URL, element presence, visibility)
 */
public class SeleniumBrowserRunner implements IBrowserRunner, DataUtils {

	private static final Logger logger = LoggerFactory.getLogger(SeleniumBrowserRunner.class);

	private final String testId;
	private final TestInstanceEventLog eventLog;
	private final BrowserControl browserControl;
	private final boolean verboseLogging;
	private CookieManager cookieManager;

	private String url;
	private ResponseCodeHtmlUnitDriver driver;
	private JsonArray tasks;
	private String currentTask;
	private String currentCommand;
	private String lastException;
	private String placeholder;
	private String method;
	private final int delaySeconds;

	/**
	 * Create a new Selenium browser runner.
	 *
	 * @param url            URL to navigate to
	 * @param tasks          JSON array of browser automation tasks
	 * @param placeholder    Optional placeholder for image/screenshot logging
	 * @param method         HTTP method (GET or POST)
	 * @param delaySeconds   Delay in seconds before starting navigation
	 * @param testId         Test instance ID
	 * @param eventLog       Event log for recording actions
	 * @param browserControl Reference to parent BrowserControl for callbacks
	 * @param verboseLogging Enable verbose logging of browser actions
	 */
	public SeleniumBrowserRunner(String url, JsonArray tasks, String placeholder, String method,
			int delaySeconds, String testId, TestInstanceEventLog eventLog,
			BrowserControl browserControl,
			boolean verboseLogging) {
		this.url = url;
		this.tasks = tasks;
		this.placeholder = placeholder;
		this.method = method;
		this.delaySeconds = delaySeconds;
		this.testId = testId;
		this.eventLog = eventLog;
		this.browserControl = browserControl;
		this.cookieManager = new CookieManager();
		this.verboseLogging = verboseLogging;

		// Each runner gets its own driver for thread safety
		this.driver = new ResponseCodeHtmlUnitDriver();
	}

	@Override
	public String call() {
		try {
			logger.info(testId + ": Sending BrowserControl to: " + url);

			try {
				Thread.sleep(delaySeconds * 1000L);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			if (Objects.equals(method, "POST")) {

				URL urlWithQueryString = new URL(url);
				URL urlWithoutQuery = new URL(urlWithQueryString.getProtocol(), urlWithQueryString.getHost(),
						urlWithQueryString.getPort(), urlWithQueryString.getPath());
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
						"browser", "goToUrl"));

				// do the actual HTTP POST
				client.getPage(request);

			} else {

				eventLog.log("WebRunner", args(
						"msg", "Scripted browser HTTP request",
						"http", "request",
						"request_uri", url,
						"request_method", method,
						"browser", "goToUrl"));

				// do the actual HTTP GET
				driver.get(url);

			}

			eventLog.log("WebRunner", args(
					"msg", "Scripted browser HTTP response",
					"http", "response",
					"response_status_code", driver.getResponseCode(),
					"response_status_text", driver.getStatus(),
					"response_content_type", driver.getResponseContentType(),
					"response_content", driver.getResponseContent()));

			// Consider this URL visited
			browserControl.urlVisited(url);

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
									"commands", currentTask.get("commands")));

							skip = true; // we're going to skip this command
						} else {
							eventLog.log("WebRunner", args(
									"msg", "Unexpected URL for non-optional task",
									"match", expectedUrlMatcher,
									"url", driver.getCurrentUrl(),
									"result", Condition.ConditionResult.FAILURE,
									"task", taskName,
									"commands", currentTask.get("commands")));

							throw new TestFailureException(testId, "WebRunner unexpected url for task: "
									+ OIDFJSON.getString(currentTask.get("task")));
						}
					}

				}

				// if it does run the commands
				if (!skip) {
					JsonArray commands = currentTask.getAsJsonArray("commands");
					if (commands != null) { // we can have zero commands to just do a check that currentUrl is what we
											// expect

						// wait for webpage to finish loading
						WebDriverWait waiting = new WebDriverWait(driver, Duration.ofSeconds(10),
								Duration.ofMillis(100));
						try {
							waiting.until((ExpectedCondition<Boolean>) webDriver -> ((JavascriptExecutor) webDriver)
									.executeScript("return document.readyState").equals("complete"));
						} catch (TimeoutException timeoutException) {
							logger.error(testId + ": WebRunner caught exception: ", timeoutException);
							eventLog.log("BROWSER",
									ex(timeoutException, Map.of("msg", "Timeout waiting for page to load")));
						}

						// execute all of the commands in this task
						for (int j = 0; j < commands.size(); j++) {
							doCommand(commands.get(j).getAsJsonArray(), taskName);
							// clear the current command once it's done
							this.currentCommand = null;
						}
					}

					// Check the server response (Completing all browser command tasks should result
					// in a submit/new page.)

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
							"response_status_text", driver.getStatus()));
				} // if we don't run the commands, just go straight to the next one
			}
			logger.debug(testId + ": Completed Browser Commands");

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
			browserControl.removeRunner(this);
			driver.close();
		}
	}

	/**
	 * Execute a single browser command from JSON configuration.
	 *
	 * Command format: ["command_type", "selector_type", "target", ...args]
	 *
	 * Supported commands:
	 * - click: ["click", selector_type, target, "optional"]
	 * - text: ["text", selector_type, target, value, "optional"]
	 * - wait: ["wait", "contains"|"match", pattern, timeout_seconds]
	 * or ["wait", selector_type, target, timeout_seconds, regexp,
	 * "update-image-placeholder"]
	 * - wait-element-visible: ["wait-element-visible", selector_type, target,
	 * timeout_seconds]
	 * - wait-element-invisible: ["wait-element-invisible", selector_type, target,
	 * timeout_seconds]
	 *
	 * @param command  JSON array representing the command
	 * @param taskName Name of current task for logging
	 * @throws TestFailureException if command fails or is invalid
	 */
	private void doCommand(JsonArray command, String taskName) {
		// general format for command is [command_string, element_id_type, element_id,
		// other_args]
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
						"result", Condition.ConditionResult.INFO));

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
								"result", Condition.ConditionResult.INFO));
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
						"result", Condition.ConditionResult.INFO));

				try {
					WebElement entryBox = driver.findElement(getSelector(elementType, target));

					entryBox.clear();
					entryBox.sendKeys(value);
					logger.debug(
							testId + ":\t\tEntered text: '" + value + "' into " + target + " (" + elementType + ")");
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
								"result", Condition.ConditionResult.INFO));
					} else {
						throw e;
					}
				}

			} else if (commandString.equalsIgnoreCase("wait")) {
				// ["wait","match" or "contains",
				// "urlmatch_or_contains_string",timeout_in_seconds]
				// 'wait' will wait for the URL to match a regex, or for it to contain a string,
				// OR
				// 'wait' can wait for the presence of an element (like a button) using the same
				// selectors (id, name) as click and text above.
				// if waiting for an element, the next parameter can be a regexp to be matched
				// and the final parameter can be 'update-image-placeholder' to mark an image
				// placeholder as satisfied

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
						"action", action));
				// hook to wait for this condition, check every 100 milliseconds until the max
				// seconds
				WebDriverWait waiting = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds),
						Duration.ofMillis(100));
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
							browserControl.updatePlaceholder(this.placeholder, driver.getPageSource(),
									driver.getResponseContentType(), regexp, updateImagePlaceHolderOptional);
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
					WebDriverWait waiting = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds),
							Duration.ofMillis(100));
					waiting.until(ExpectedConditions.invisibilityOfElementLocated(getSelector(elementType, target)));
					logger.debug(testId + ":\t\tElement with " + elementType + " '" + target + "' is now invisible");
				} catch (TimeoutException timeoutException) {
					this.lastException = timeoutException.getMessage();
					throw new TestFailureException(testId,
							"Timed out waiting for element to become invisible: " + command.toString());
				}
			} else if (commandString.equalsIgnoreCase("wait-element-visible")) {
				int timeoutSeconds = OIDFJSON.getInt(command.get(3));
				try {
					WebDriverWait waiting = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds),
							Duration.ofMillis(100));
					waiting.until(ExpectedConditions.visibilityOfElementLocated(getSelector(elementType, target)));
					logger.debug(testId + ":\t\tElement with " + elementType + " '" + target + "' is now visible");
				} catch (TimeoutException timeoutException) {
					this.lastException = timeoutException.getMessage();
					throw new TestFailureException(testId,
							"Timed out waiting for element visibility: " + command.toString());
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
	 * Convert selector type and target value to Selenium By selector.
	 *
	 * Supported selector types:
	 * - id: By.id()
	 * - name: By.name()
	 * - xpath: By.xpath()
	 * - css: By.cssSelector()
	 * - class: By.className()
	 *
	 * @param type  Selector type (id, name, xpath, css, class)
	 * @param value Selector target value
	 * @return Selenium By selector
	 * @throws TestFailureException if selector type is invalid
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

	@Override
	public String getCurrentTask() {
		return currentTask;
	}

	@Override
	public String getCurrentCommand() {
		return currentCommand;
	}

	@Override
	public String getLastException() {
		return lastException;
	}

	// Package-private accessors for BrowserControl.getWebRunners()
	String getUrl() {
		return url;
	}

	String getCurrentUrl() {
		return driver.getCurrentUrl();
	}

	int getResponseCode() {
		return driver.getResponseCode();
	}

	String getResponseStatus() {
		return driver.getStatus();
	}

	String getResponseContentType() {
		return driver.getResponseContentType();
	}

	String getResponseContent() {
		return driver.getResponseContent();
	}

	String getLastResponseCode() {
		return String.valueOf(driver.getResponseCode());
	}

	// Inner classes for HtmlUnit driver customization

	/**
	 * Custom page creator that treats all responses as HTML.
	 * This is necessary because some authorization servers return incorrect
	 * content-type headers.
	 */
	@SuppressWarnings("serial")
	private static class BrowserControlPageCreator extends org.htmlunit.DefaultPageCreator {
		// this is necessary because:
		// curl -v
		// 'https://fapidev-as.authlete.net/api/authorization?client_id=21541757519&redirect_uri=https://localhost:8443/test/a/authlete-fapi/callback&scope=openid%20accounts&state=ND4WAuQ8lt&nonce=lOgNDes2YE&response_type=code%20id_token'
		// returns:
		// Content-Type: */*;charset=utf-8
		// so we need to override this so it's treated as html, which is how browsers
		// treat it
		@Override
		public org.htmlunit.Page createPage(final org.htmlunit.WebResponse webResponse,
				final org.htmlunit.WebWindow webWindow) throws java.io.IOException {
			return createHtmlPage(webResponse, webWindow);
		}
	}

	/**
	 * Custom HTTP connection that logs all requests and responses to the event log.
	 */
	private class LoggingHttpWebConnection extends org.htmlunit.HttpWebConnection {

		public LoggingHttpWebConnection(WebClient webClient) {
			super(webClient);
		}

		/**
		 * Convert headers returned by HTMLUnit into a JsonObject
		 */
		public JsonObject mapHeadersToJsonObject(java.util.List<org.htmlunit.util.NameValuePair> headers) {
			JsonObject o = new JsonObject();
			for (org.htmlunit.util.NameValuePair pair : headers) {
				String name = pair.getName();
				if (o.has(name)) {
					// If header occurs multiple times, put each value as an array element
					JsonArray array;
					com.google.gson.JsonElement existing = o.get(name);
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
		public org.htmlunit.WebResponse getResponse(org.htmlunit.WebRequest webRequest) throws java.io.IOException {
			eventLog.log("WebRunner", args(
					"msg", "Request " + webRequest.getHttpMethod() + " " + webRequest.getUrl(),
					"headers", webRequest.getAdditionalHeaders(),
					"params", webRequest.getRequestParameters(),
					"body", webRequest.getRequestBody(),
					"result", Condition.ConditionResult.INFO));

			org.htmlunit.WebResponse response = super.getResponse(webRequest);

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
	 * SubClass of {@link org.openqa.selenium.htmlunit.HtmlUnitDriver} to provide
	 * access to the response code of the last page we visited
	 */
	class ResponseCodeHtmlUnitDriver extends org.openqa.selenium.htmlunit.HtmlUnitDriver {

		public ResponseCodeHtmlUnitDriver() {
			super(true);
			final org.htmlunit.WebConsole console = getWebClient().getWebConsole();
			console.setLogger(new org.htmlunit.WebConsole.Logger() {
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
			org.htmlunit.html.HtmlPage page = (org.htmlunit.html.HtmlPage) this.getCurrentWindow().lastPage();
			return page.getDocumentElement().asXml();
		}

		public String getStatus() {
			String responseCodeString = this.getCurrentWindow().lastPage().getWebResponse().getStatusCode() + "-" +
					this.getCurrentWindow().lastPage().getWebResponse().getStatusMessage();
			return responseCodeString;
		}

		@Override
		protected WebClient newWebClient(org.htmlunit.BrowserVersion version) {
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

			client.setJavaScriptErrorListener(new org.htmlunit.javascript.JavaScriptErrorListener() {

				@Override
				public void scriptException(org.htmlunit.html.HtmlPage page,
						org.htmlunit.ScriptException scriptException) {
					eventLog.log("BROWSER",
							args("msg", "Error during JavaScript execution", "detail", scriptException.toString()));
				}

				@Override
				public void timeoutError(org.htmlunit.html.HtmlPage page, long allowedTime, long executionTime) {
					eventLog.log("BROWSER", args("msg", "Timeout during JavaScript execution after "
							+ executionTime + "ms; allowed only " + allowedTime + "ms"));

				}

				@Override
				public void malformedScriptURL(org.htmlunit.html.HtmlPage page, String url,
						java.net.MalformedURLException malformedURLException) {
					eventLog.log("BROWSER", args("msg", "Unable to build URL for script src tag [" + url + "]",
							"exception", malformedURLException.toString()));
				}

				@Override
				public void loadScriptError(org.htmlunit.html.HtmlPage page, java.net.URL scriptUrl,
						Exception exception) {
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
}
