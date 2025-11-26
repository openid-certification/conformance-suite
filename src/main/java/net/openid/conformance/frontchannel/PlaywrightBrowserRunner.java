package net.openid.conformance.frontchannel;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.Page.WaitForSelectorOptions;
import com.microsoft.playwright.options.RequestOptions;
import com.microsoft.playwright.options.WaitForSelectorState;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.DataUtils;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.PatternMatchUtils;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonParser;

/**
 * Playwright-based browser automation runner.
 * Uses real browser engines (Chromium, Firefox, or WebKit) for JavaScript-heavy
 * pages.
 *
 * Executes browser commands from JSON configuration including:
 * - Navigation (GET/POST)
 * - Element interaction (clicks, text input)
 * - Waiting conditions (URL, element presence, visibility)
 *
 * Note: To maintain compatibility with Selenium WebDriver behavior, all element
 * locators use .first() to automatically select the first matching element when
 * multiple elements match a selector. This replicates Selenium's findElement()
 * which implicitly returns the first match, avoiding Playwright's strict mode
 * violations.
 */
public class PlaywrightBrowserRunner implements IBrowserRunner, DataUtils {

	private static final Logger logger = LoggerFactory.getLogger(PlaywrightBrowserRunner.class);

	private final String testId;
	private final TestInstanceEventLog eventLog;
	private final BrowserControl browserControl;
	private final boolean headless;
	private final String browserType;
	private final int slowMo;
	private final Map<String, String> extraHttpHeaders;

	private String url;
	private Playwright playwright;
	private Browser browser;
	private BrowserContext context;
	private Page page;
	private JsonArray tasks;
	private String currentTask;
	private String currentCommand;
	private String lastException;
	private String placeholder;
	private String method;
	private final int delaySeconds;

	/**
	 * Create a new Playwright browser runner.
	 *
	 * @param url            URL to navigate to
	 * @param tasks          JSON array of browser automation tasks
	 * @param placeholder    Optional placeholder for image/screenshot logging
	 * @param method         HTTP method (GET or POST)
	 * @param delaySeconds   Delay in seconds before starting navigation
	 * @param testId         Test instance ID
	 * @param eventLog       Event log for recording actions
	 * @param browserControl Reference to parent BrowserControl for callbacks
	 */
	public PlaywrightBrowserRunner(String url, JsonArray tasks, String placeholder, String method,
			int delaySeconds, String testId, TestInstanceEventLog eventLog,
			BrowserControl browserControl) {
		this.url = url;
		this.tasks = tasks;
		this.placeholder = placeholder;
		this.method = method;
		this.delaySeconds = delaySeconds;
		this.testId = testId;
		this.eventLog = eventLog;
		this.browserControl = browserControl;
		this.headless = Boolean.parseBoolean(System.getProperty("fintechlabs.browser.playwright.headless", "true"));
		this.browserType = System.getProperty("fintechlabs.browser.playwright.type", "chromium").toLowerCase();
		this.slowMo = Integer.parseInt(System.getProperty("fintechlabs.browser.playwright.slowMo", "1000"));
		this.extraHttpHeaders = parseExtraHttpHeaders(
				System.getProperty("fintechlabs.browser.playwright.extraHttpHeaders", ""));

		logger.info("Playwright browser type: " + this.browserType + ", headless: "
				+ this.headless + ", slowMo: " + this.slowMo + "ms");
	}

	@Override
	public String call() {
		try {
			logger.info(testId + ": Sending Playwright BrowserControl to: " + url);

			// Initialize Playwright and browser
			initializeBrowser();

			try {
				Thread.sleep(delaySeconds * 1000L);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			// Navigate to URL
			if (Objects.equals(method, "POST")) {
				URL urlWithQueryString = new URL(url);
				URL urlWithoutQuery = new URL(urlWithQueryString.getProtocol(), urlWithQueryString.getHost(),
						urlWithQueryString.getPort(), urlWithQueryString.getPath());
				String params = urlWithQueryString.getQuery();
				page.request().fetch(urlWithoutQuery.toString(),
						RequestOptions.create()
								.setMethod("POST")
								.setHeader("Content-Type", "application/x-www-form-urlencoded")
								.setData(params));

				eventLog.log("PlaywrightRunner", args(
						"msg", "Scripted browser HTTP request",
						"http", "request",
						"request_uri", urlWithoutQuery.toString(),
						"parameters", params,
						"request_method", method,
						"browser", "goToUrl"));

			} else {
				eventLog.log("PlaywrightRunner", args(
						"msg", "Scripted browser HTTP request",
						"http", "request",
						"request_uri", url,
						"request_method", method,
						"browser", "goToUrl"));

				page.navigate(url);
			}
			page.waitForLoadState();

			eventLog.log("PlaywrightRunner", args(
					"msg", "Scripted browser HTTP response",
					"http", "response",
					"url", page.url(),
					"title", page.title()));

			// Consider this URL visited
			browserControl.urlVisited(url);

			// Execute tasks
			for (int i = 0; i < this.tasks.size(); i++) {
				boolean skip = false;

				JsonObject currentTask = this.tasks.get(i).getAsJsonObject();

				if (currentTask.get("task") == null) {
					throw new TestFailureException(testId, "Invalid Task Definition: no 'task' property");
				}

				String taskName = OIDFJSON.getString(currentTask.get("task"));
				this.currentTask = taskName;

				logger.debug(testId + ": Performing: " + taskName);
				logger.debug(testId + ": PlaywrightRunner current url:" + page.url());

				// Check if current URL matches the 'matcher' for the task
				String expectedUrlMatcher = "*"; // default to matching any URL
				if (currentTask.has("match")) {
					expectedUrlMatcher = OIDFJSON.getString(currentTask.get("match"));
				}

				if (!Strings.isNullOrEmpty(expectedUrlMatcher)) {
					if (!PatternMatchUtils.simpleMatch(expectedUrlMatcher, page.url())) {
						if (currentTask.has("optional") && OIDFJSON.getBoolean(currentTask.get("optional"))) {
							eventLog.log("PlaywrightRunner", args(
									"msg", "Skipping optional task due to URL mismatch",
									"match", expectedUrlMatcher,
									"url", page.url(),
									"browser", "skip",
									"task", taskName,
									"commands", currentTask.get("commands")));

							skip = true;
						} else {
							eventLog.log("PlaywrightRunner", args(
									"msg", "Unexpected URL for non-optional task",
									"match", expectedUrlMatcher,
									"url", page.url(),
									"result", Condition.ConditionResult.FAILURE,
									"task", taskName,
									"commands", currentTask.get("commands")));

							throw new TestFailureException(testId, "PlaywrightRunner unexpected url for task: "
									+ OIDFJSON.getString(currentTask.get("task")));
						}
					}
				}

				// Execute commands if not skipped
				if (!skip) {
					JsonArray commands = currentTask.getAsJsonArray("commands");
					if (commands != null) {
						// Check if task contains URL wait commands (contains/match)
						// If so, skip waitForLoadState since we're expecting navigation
						boolean hasUrlWaitCommand = false;
						for (int j = 0; j < commands.size(); j++) {
							JsonArray cmd = commands.get(j).getAsJsonArray();
							if (cmd.size() >= 3) {
								String cmdType = OIDFJSON.getString(cmd.get(0));
								String elementType = OIDFJSON.getString(cmd.get(1));
								if (cmdType != null && cmdType.equalsIgnoreCase("wait") &&
										elementType != null && (elementType.equalsIgnoreCase("contains")
												|| elementType.equalsIgnoreCase("match"))) {
									hasUrlWaitCommand = true;
									break;
								}
							}
						}

						// Wait for page to be ready only if not waiting for navigation
						if (!hasUrlWaitCommand) {
							page.waitForLoadState();
						}

						// Execute all commands in this task
						for (int j = 0; j < commands.size(); j++) {
							doCommand(commands.get(j).getAsJsonArray(), taskName);
							this.currentCommand = null;
						}
					}

					eventLog.log("PlaywrightRunner", args(
							"msg", "Completed processing of webpage",
							"match", expectedUrlMatcher,
							"url", page.url(),
							"browser", "complete",
							"task", taskName,
							"result", Condition.ConditionResult.INFO));
				}
			}

			logger.debug(testId + ": Completed Browser Commands");

			return "playwright runner exited";
		} catch (Exception | Error e) {
			logger.error(testId + ": PlaywrightRunner caught exception", e);

			String pageContent = "";
			try {
				pageContent = page != null ? page.content() : "";
			} catch (Exception contentEx) {
				logger.warn("Could not retrieve page content", contentEx);
			}

			eventLog.log("PlaywrightRunner",
					ex(e,
							args("msg", e.getMessage(),
									"page_source", pageContent,
									"url", page != null ? page.url() : url,
									"result", Condition.ConditionResult.FAILURE)));

			this.lastException = e.getMessage();
			if (e instanceof TestFailureException) {
				throw new TestFailureException(testId, "Playwright Runner Exception: " + e.getMessage(), e.getCause());
			}
			throw new TestFailureException(testId, "Playwright Runner Exception: " + e.getMessage(), e);
		} finally {
			browserControl.removeRunner(this);
			closeBrowser();
		}
	}

	/**
	 * Initialize Playwright and launch browser
	 */
	private void initializeBrowser() {
		playwright = Playwright.create();

		BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
				.setHeadless(headless)
				.setSlowMo(slowMo);

		// Launch appropriate browser type
		switch (browserType) {
			case "firefox":
				browser = playwright.firefox().launch(launchOptions);
				break;
			case "webkit":
				browser = playwright.webkit().launch(launchOptions);
				break;
			case "chromium":
			default:
				browser = playwright.chromium().launch(launchOptions);
				break;
		}

		// Create browser context with HTTPS error ignore
		context = browser.newContext(new Browser.NewContextOptions()
				.setIgnoreHTTPSErrors(true));

		// Set default timeout (30 seconds)
		context.setDefaultTimeout(30_000);

		// Create page
		page = context.newPage();

		// Set extra HTTP headers if configured
		if (!extraHttpHeaders.isEmpty()) {
			page.setExtraHTTPHeaders(extraHttpHeaders);
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
	 * or ["wait", selector_type, target, timeout_seconds, regexp]
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
		String commandString = OIDFJSON.getString(command.get(0));
		if (Strings.isNullOrEmpty(commandString)) {
			this.lastException = "Invalid Command: empty command string";
			throw new TestFailureException(testId, "Invalid Command: empty command string");
		}

		this.currentCommand = commandString;

		String elementType = OIDFJSON.getString(command.get(1));
		String target = OIDFJSON.getString(command.get(2));

		if (commandString.equalsIgnoreCase("click")) {
			String selector = getSelector(elementType, target);

			eventLog.log("PlaywrightRunner", args(
					"msg", "Clicking an element",
					"url", page.url(),
					"browser", commandString,
					"task", taskName,
					"element_type", elementType,
					"target", target,
					"playwright_selector", selector,
					"result", Condition.ConditionResult.INFO));

			try {
				getLocator(elementType, target).click();
				logger.debug(testId + ": Successfully clicked: " + target + " (" + elementType + ")");
			} catch (Exception e) {
				String optional = command.size() >= 4 ? OIDFJSON.getString(command.get(3)) : null;
				if (optional != null && optional.equals("optional")) {
					eventLog.log("PlaywrightRunner", args(
							"msg", "Element not found, skipping as 'click' command is marked 'optional'",
							"url", page.url(),
							"browser", commandString,
							"task", taskName,
							"element_type", elementType,
							"target", target,
							"playwright_selector", selector,
							"result", Condition.ConditionResult.INFO));
				} else {
					logger.error(testId + ": Failed to click element with selector: " + selector + ", error: "
							+ e.getMessage());
					throw e;
				}
			}

			logger.debug(testId + ": Clicked: " + target + " (" + elementType + ")");
		} else if (commandString.equalsIgnoreCase("text")) {
			String value = OIDFJSON.getString(command.get(3));

			eventLog.log("PlaywrightRunner", args(
					"msg", "Entering text",
					"url", page.url(),
					"browser", commandString,
					"task", taskName,
					"element_type", elementType,
					"target", target,
					"value", value,
					"result", Condition.ConditionResult.INFO));

			try {
				Locator locator = getLocator(elementType, target);
				locator.clear();
				locator.fill(value);
				logger.debug(testId + ":\t\tEntered text: '" + value + "' into " + target + " (" + elementType + ")");
			} catch (Exception e) {
				String optional = command.size() >= 5 ? OIDFJSON.getString(command.get(4)) : null;
				if (optional != null && optional.equals("optional")) {
					eventLog.log("PlaywrightRunner", args(
							"msg", "Element not found, skipping as 'text' command is marked 'optional'",
							"url", page.url(),
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

			eventLog.log("PlaywrightRunner", args(
					"msg", "Waiting",
					"url", page.url(),
					"browser", commandString,
					"task", taskName,
					"element_type", elementType,
					"target", target,
					"seconds", timeoutSeconds,
					"result", Condition.ConditionResult.INFO,
					"regexp", regexp,
					"action", action));

			try {
				if (elementType.equalsIgnoreCase("contains")) {
					page.waitForCondition(() -> url.contains(target));
				} else if (elementType.equalsIgnoreCase("match")) {
					page.waitForURL(url);
				} else if (!Strings.isNullOrEmpty(regexp)) {
					// Wait for element with text matching regexp
					Locator locator = getLocator(elementType, target);
					locator.waitFor(new Locator.WaitForOptions()
							.setState(WaitForSelectorState.ATTACHED)
							.setTimeout(timeoutSeconds * 1000.0));

					String text = locator.textContent();
					if (text != null && !text.matches(regexp)) {
						throw new TestFailureException(testId, "Element text does not match pattern: " + regexp);
					}

					if (updateImagePlaceHolder || updateImagePlaceHolderOptional) {
						browserControl.updatePlaceholder(this.placeholder, page.content(),
								"text/html", regexp, updateImagePlaceHolderOptional);
					}
				} else {
					page.waitForSelector(getSelector(elementType, target),
							new WaitForSelectorOptions()
									.setState(WaitForSelectorState.ATTACHED)
									.setTimeout(timeoutSeconds * 1000.0));
				}

				logger.debug(testId + ":\t\tDone waiting: " + commandString);

			} catch (TimeoutError timeoutException) {
				this.lastException = timeoutException.getMessage();
				throw new TestFailureException(testId, "Timed out waiting: " + command.toString());
			}

		} else if (commandString.equalsIgnoreCase("wait-element-invisible")) {
			int timeoutSeconds = OIDFJSON.getInt(command.get(3));
			try {
				page.waitForSelector(getSelector(elementType, target),
						new WaitForSelectorOptions()
								.setState(WaitForSelectorState.HIDDEN)
								.setTimeout(timeoutSeconds));
				logger.debug(testId + ":\t\tElement with " + elementType + " '" + target + "' is now invisible");
			} catch (TimeoutError timeoutException) {
				this.lastException = timeoutException.getMessage();
				throw new TestFailureException(testId,
						"Timed out waiting for element to become invisible: " + command.toString());
			}

		} else if (commandString.equalsIgnoreCase("wait-element-visible")) {
			int timeoutSeconds = OIDFJSON.getInt(command.get(3));
			try {
				page.waitForSelector(getSelector(elementType, target),
						new WaitForSelectorOptions()
								.setState(WaitForSelectorState.VISIBLE)
								.setTimeout(timeoutSeconds));
				logger.debug(testId + ":\t\tElement with " + elementType + " '" + target + "' is now invisible");
			} catch (TimeoutError timeoutException) {
				this.lastException = timeoutException.getMessage();
				throw new TestFailureException(testId,
						"Timed out waiting for element to become invisible: " + command.toString());
			}
		} else {
			this.lastException = "Invalid Command " + commandString;
			throw new TestFailureException(testId, "Invalid Command: " + commandString);
		}
	}

	/**
	 * Convert selector type and target value to Playwright selector string.
	 *
	 * Supported selector types:
	 * - id: #id
	 * - name: [name='value']
	 * - xpath: xpath=//path
	 * - css: css selector
	 * - class: .classname
	 *
	 * @param type  Selector type (id, name, xpath, css, class)
	 * @param value Selector target value
	 * @return Playwright selector string
	 * @throws TestFailureException if selector type is invalid
	 */
	private String getSelector(String type, String value) {
		switch (type.toLowerCase()) {
			case "id":
				return "#" + value;
			case "name":
				return "[name='" + value + "']";
			case "xpath":
				return "xpath=" + value;
			case "css":
				return value;
			case "class":
				return "." + value;
			default:
				this.lastException = "Invalid Command Selector: Type: " + type + " Value: " + value;
				throw new TestFailureException(testId, "Invalid Command Selector: Type: " + type + " Value: " + value);
		}
	}

	/**
	 * Get a Playwright locator that matches Selenium's findElement behavior.
	 * Always returns the first matching element to maintain compatibility with
	 * Selenium WebDriver's findElement() which implicitly returns the first match.
	 *
	 * This prevents Playwright's strict mode violations when a selector matches
	 * multiple elements (e.g., xpath=//*).
	 *
	 * @param type  Selector type (id, name, xpath, css, class)
	 * @param value Selector target value
	 * @return Playwright Locator configured to return first matching element
	 * @throws TestFailureException if selector type is invalid
	 */
	private Locator getLocator(String type, String value) {
		String selector = getSelector(type, value);
		return page.locator(selector).first();
	}

	/**
	 * Parse extra HTTP headers from a JSON string.
	 *
	 * @param jsonString JSON object string with header name-value pairs
	 * @return Map of header names to values, or empty map if parsing fails
	 */
	private Map<String, String> parseExtraHttpHeaders(String jsonString) {
		if (Strings.isNullOrEmpty(jsonString)) {
			return Collections.emptyMap();
		}
		try {
			Map<String, String> headers = new HashMap<>();
			JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
			for (String key : json.keySet()) {
				headers.put(key, OIDFJSON.getString(json.get(key)));
			}
			return Collections.unmodifiableMap(headers);
		} catch (Exception e) {
			logger.warn("Failed to parse extraHttpHeaders JSON: " + e.getMessage());
			return Collections.emptyMap();
		}
	}

	/**
	 * Close browser and clean up resources
	 */
	private void closeBrowser() {
		try {
			if (page != null) {
				page.close();
			}
			if (context != null) {
				context.close();
			}
			if (browser != null) {
				browser.close();
			}
			if (playwright != null) {
				playwright.close();
			}
		} catch (Exception e) {
			logger.warn(testId + ": Error closing Playwright browser", e);
		}
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
		return page != null ? page.url() : url;
	}
}
