package net.openid.conformance.frontchannel;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.DataUtils;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A {@link BrowserRunner} driving a real browser engine (Chromium, Firefox or WebKit) through
 * <a href="https://playwright.dev/java/">Playwright</a>. Selected with {@code -Dbrowser.engine=playwright};
 * see {@link Settings} for the other {@code browser.playwright.*} system properties.
 *
 * <p>The runner understands the same "browser" configuration block as {@link SeleniumBrowserRunner}
 * and logs under the same {@code WebRunner} source so tooling that inspects the test log
 * (expected-failure lists, scripts/compare-results.py) works with either engine.
 *
 * <p>Locators always take the first matching element, mirroring Selenium's {@code findElement()},
 * so selectors that match several elements (e.g. {@code xpath=//*}) don't trip Playwright's strict
 * mode. Each runner launches its own browser; cookies and storage are carried between the runners of
 * one test module instance via the context's storage state held by {@link BrowserControl}, which
 * gives the same "shared cookie jar" behaviour the OIDC prompt=login tests rely on with HtmlUnit.
 */
public class PlaywrightBrowserRunner implements BrowserRunner, DataUtils {

	private static final Logger logger = LoggerFactory.getLogger(PlaywrightBrowserRunner.class);

	/**
	 * Timeout for navigations (the initial visit and the navigation a click sets off), and the
	 * default for anything not given an explicit timeout below. A navigation spans the whole
	 * chain of the system under test processing the request, redirecting and the suite's
	 * endpoint answering, so this matches the HtmlUnit runner's HTTP timeout
	 * ({@code SeleniumBrowserRunner.BROWSER_HTTP_TIMEOUT_MILLIS}).
	 */
	private static final double NAVIGATION_TIMEOUT_MILLIS = 60_000;
	/** Timeout for the target element of a click/text command to appear. */
	private static final double ELEMENT_TIMEOUT_MILLIS = 10_000;
	/** Timeout for the page to finish loading before a task's commands run (matches the HtmlUnit runner). */
	private static final double PAGE_LOAD_TIMEOUT_MILLIS = 10_000;

	private final BrowserControl browserControl;
	private final String testId;
	private final TestInstanceEventLog eventLog;
	private final Settings settings;
	private final BrowserVisit visit;

	private Playwright playwright;
	private Browser browser;
	private BrowserContext context;
	private Page page;

	private String currentTask;
	private String currentCommand;
	private String lastException;
	private boolean failed = false;

	// Playwright objects must only be touched from the runner thread, so getStatus() (called from
	// the HTTP request thread) serves these cached copies instead.
	private volatile String cachedCurrentUrl;
	private volatile String cachedScreenshot;

	/**
	 * @param browserControl the owning control, notified when the url has been visited and when this runner finishes
	 * @param testId         id of the test instance
	 * @param eventLog       test log to record the browser's actions in
	 * @param settings       engine settings (browser type, headless, tracing, ...)
	 * @param visit          the url, tasks and options to run
	 */
	PlaywrightBrowserRunner(BrowserControl browserControl, String testId, TestInstanceEventLog eventLog,
							Settings settings, BrowserVisit visit) {
		this.browserControl = browserControl;
		this.testId = testId;
		this.eventLog = eventLog;
		this.settings = settings;
		this.visit = visit;
	}

	@Override
	public String call() {
		String url = visit.url();
		String method = visit.method();
		try {
			logger.info(testId + ": Sending Playwright BrowserControl (" + settings.browserType() + ") to: " + url);

			launchBrowser();

			try {
				Thread.sleep(visit.delaySeconds() * 1000L);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			Response response = null;
			if (Objects.equals(method, "POST")) {
				URI uri = URI.create(url);
				String urlWithoutQuery = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null).toString();
				String params = uri.getRawQuery();

				eventLog.log("WebRunner", args(
					"msg", "Scripted browser HTTP request",
					"http", "request",
					"request_uri", urlWithoutQuery,
					"parameters", params,
					"request_method", method,
					"browser", "goToUrl",
					"browser_engine", engineDescription()
				));

				// A real browser can't issue a top-level POST directly; submit it the way a web page
				// would, through a form.
				page.setContent(buildAutoSubmittingForm(urlWithoutQuery, params));
				page.waitForURL(u -> !"about:blank".equals(u), new Page.WaitForURLOptions()
					.setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
					.setTimeout(NAVIGATION_TIMEOUT_MILLIS));
			} else {
				eventLog.log("WebRunner", args(
					"msg", "Scripted browser HTTP request",
					"http", "request",
					"request_uri", url,
					"request_method", method,
					"browser", "goToUrl",
					"browser_engine", engineDescription()
				));

				// DOMContentLoaded rather than the default 'load': see the per-task wait below
				response = page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
			}
			updateCache();

			eventLog.log("WebRunner", args(
				"msg", "Scripted browser HTTP response",
				"http", "response",
				"url", page.url(),
				"title", page.title(),
				"response_status_code", response != null ? response.status() : null,
				"response_content_type", response != null ? response.headers().get("content-type") : null,
				"img", cachedScreenshot
			));

			// Consider this URL visited
			browserControl.urlVisited(url);

			JsonArray tasks = visit.tasks();
			for (int i = 0; i < tasks.size(); i++) {
				boolean skip = false;

				JsonObject currentTask = tasks.get(i).getAsJsonObject();

				if (currentTask.get("task") == null) {
					throw new TestFailureException(testId, "Invalid Task Definition: no 'task' property");
				}

				String taskName = OIDFJSON.getString(currentTask.get("task"));
				this.currentTask = taskName;

				logger.debug(testId + ": Performing: " + taskName);
				logger.debug(testId + ": WebRunner current url:" + page.url());

				// check if current URL matches the 'matcher' for the task
				String expectedUrlMatcher = "*"; // default to matching any URL
				if (currentTask.has("match")) {
					// if there is a more specific "match" element, use its value instead
					expectedUrlMatcher = OIDFJSON.getString(currentTask.get("match"));
				}

				if (!Strings.isNullOrEmpty(expectedUrlMatcher)) {
					if (!PatternMatchUtils.simpleMatch(expectedUrlMatcher, page.url())) {
						if (currentTask.has("optional") && OIDFJSON.getBoolean(currentTask.get("optional"))) {
							eventLog.log("WebRunner", args(
								"msg", "Skipping optional task due to URL mismatch",
								"match", expectedUrlMatcher,
								"url", page.url(),
								"browser", "skip",
								"task", taskName,
								"commands", currentTask.get("commands")
							));

							skip = true; // we're going to skip this command
						} else {
							eventLog.log("WebRunner", args(
								"msg", "Unexpected URL for non-optional task",
								"match", expectedUrlMatcher,
								"url", page.url(),
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

						// wait for the webpage's DOM to be ready. DOMContentLoaded rather than the load
						// event: 'load' also waits for every subresource and hangs on pages that keep
						// a request open, while the commands below auto-wait for their elements anyway
						try {
							page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(PAGE_LOAD_TIMEOUT_MILLIS));
						} catch (TimeoutError timeoutException) {
							logger.warn(testId + ": WebRunner timed out waiting for the page to load, continuing with task '" + taskName + "' on " + page.url());
							eventLog.log("BROWSER", ex(timeoutException, Map.of("msg", "Timeout waiting for page to load")));
						}

						// execute all of the commands in this task
						for (int j = 0; j < commands.size(); j++) {
							doCommand(commands.get(j).getAsJsonArray(), taskName);
							// clear the current command once it's done
							this.currentCommand = null;
							updateCache();
						}
					}

					eventLog.log("WebRunner", args(
						"msg", "Completed processing of webpage",
						"match", expectedUrlMatcher,
						"url", page.url(),
						"browser", "complete",
						"task", taskName,
						"result", Condition.ConditionResult.INFO
					));
				} // if we don't run the commands, just go straight to the next one
			}
			logger.debug(testId + ": Completed Browser Commands");

			return "web runner exited";
		} catch (Exception | Error e) {
			if (wasInterrupted(e)) {
				// The test finished (or was cancelled) while we were still driving the browser and
				// TestExecutionManager.cancelAllBackgroundTasks() interrupted us; nothing went wrong.
				logger.info(testId + ": WebRunner stopped as the test has finished; task: " + currentTask + ", command: " + currentCommand);
				eventLog.log("WebRunner", args(
					"msg", "Scripted browser stopped as the test has finished",
					"url", cachedCurrentUrl != null ? cachedCurrentUrl : url,
					"task", currentTask,
					"command", currentCommand,
					"result", Condition.ConditionResult.INFO));
				this.lastException = "Stopped as the test has finished";
				return "web runner cancelled";
			}

			logger.error(testId + ": WebRunner caught exception", e);
			failed = true;
			updateCache();

			String pageSource = null;
			try {
				pageSource = page != null ? page.content() : null;
			} catch (RuntimeException contentEx) {
				logger.warn(testId + ": Could not retrieve page content", contentEx);
			}

			eventLog.log("WebRunner",
				ex(e,
					args("msg", e.getMessage(),
						"page_source", pageSource,
						"url", cachedCurrentUrl != null ? cachedCurrentUrl : url,
						"img", cachedScreenshot,
						"result", Condition.ConditionResult.FAILURE)));
			this.lastException = e.getMessage();
			if (e instanceof TestFailureException) {
				// avoid wrapping a TestFailureException around a TestFailureException
				throw new TestFailureException(testId, "Web Runner Exception: " + e.getMessage(), e.getCause());
			}
			throw new TestFailureException(testId, "Web Runner Exception: " + e.getMessage(), e);
		} finally {
			browserControl.removeRunner(this);
			closeBrowser();
		}
	}

	private String engineDescription() {
		return "playwright/" + settings.browserType();
	}

	private void launchBrowser() {
		playwright = Playwright.create();

		BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
			.setHeadless(settings.headless())
			.setSlowMo(settings.slowMo());

		switch (settings.browserType()) {
			case "firefox":
				browser = playwright.firefox().launch(launchOptions);
				break;
			case "webkit":
				browser = playwright.webkit().launch(launchOptions);
				break;
			case "chromium":
				browser = playwright.chromium().launch(launchOptions);
				break;
			default:
				throw new TestFailureException(testId, "Unsupported browser.playwright.type '" + settings.browserType()
					+ "', expected one of chromium, firefox, webkit");
		}

		Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
			// the suite is usually reached through a self-signed dev certificate
			.setIgnoreHTTPSErrors(true);

		// carry over cookies/storage from the previous runner of this test module instance
		String storageState = browserControl.getPlaywrightStorageState();
		if (storageState != null) {
			contextOptions.setStorageState(storageState);
		}

		Map<String, String> headers = new HashMap<>(settings.extraHttpHeaders());
		headers.putIfAbsent("ngrok-skip-browser-warning", "true");
		contextOptions.setExtraHTTPHeaders(headers);

		context = browser.newContext(contextOptions);
		context.setDefaultTimeout(NAVIGATION_TIMEOUT_MILLIS);

		if (settings.traceMode() != TraceMode.OFF) {
			context.tracing().start(new Tracing.StartOptions()
				.setScreenshots(true)
				.setSnapshots(true)
				.setSources(false));
			logger.debug(testId + ": Playwright tracing started");
		}

		page = context.newPage();
	}

	/**
	 * Given a command like '["click","id","btnId"], this will perform the Playwright calls to execute it.
	 * See {@link SeleniumBrowserRunner#doCommand} for the command formats; both runners accept the same.
	 *
	 * @throws TestFailureException if an invalid command is specified
	 */
	private void doCommand(JsonArray command, String taskName) {
		// general format for command is [command_string, element_id_type, element_id, other_args]
		String commandString = OIDFJSON.getString(command.get(0));
		Command parsedCommand = parseCommand(commandString);
		this.currentCommand = commandString;

		// selectors common to all elements
		String elementType = OIDFJSON.getString(command.get(1));
		String target = OIDFJSON.getString(command.get(2));

		switch (parsedCommand) {
			case CLICK: {
				// ["click", "id" or "name", "id_or_name"]
				eventLog.log("WebRunner", args(
					"msg", "Clicking an element",
					"url", page.url(),
					"browser", commandString,
					"task", taskName,
					"element_type", elementType,
					"target", target,
					"result", Condition.ConditionResult.INFO
				));

				Locator locator = getLocator(elementType, target);
				try {
					locator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED).setTimeout(ELEMENT_TIMEOUT_MILLIS));
				} catch (TimeoutError e) {
					String optional = command.size() >= 4 ? OIDFJSON.getString(command.get(3)) : null;
					if (optional != null && optional.equals("optional")) {
						eventLog.log("WebRunner", args(
							"msg", "Element not found, skipping as 'click' command is marked 'optional'",
							"url", page.url(),
							"browser", commandString,
							"task", taskName,
							"element_type", elementType,
							"target", target,
							"result", Condition.ConditionResult.INFO
						));
						break;
					}
					throw e;
				}

				// like HtmlUnit's synchronous click, this also waits for the navigation the click
				// sets off (if any) to commit, hence the navigation timeout
				locator.click(new Locator.ClickOptions().setTimeout(NAVIGATION_TIMEOUT_MILLIS));

				logger.debug(testId + ": Clicked: " + target + " (" + elementType + ")");
				break;
			}

			case TEXT: {
				// ["text", "id" or "name", "id_or_name", "text_to_enter", "optional"]
				String value = OIDFJSON.getString(command.get(3));

				eventLog.log("WebRunner", args(
					"msg", "Entering text",
					"url", page.url(),
					"browser", commandString,
					"task", taskName,
					"element_type", elementType,
					"target", target,
					"value", value,
					"result", Condition.ConditionResult.INFO
				));

				try {
					// fill() clears the field before entering the value
					getLocator(elementType, target).fill(value, new Locator.FillOptions().setTimeout(ELEMENT_TIMEOUT_MILLIS));
					logger.debug(testId + ":\t\tEntered text: '" + value + "' into " + target + " (" + elementType + ")");
				} catch (TimeoutError e) {
					String optional = command.size() >= 5 ? OIDFJSON.getString(command.get(4)) : null;
					if (optional != null && optional.equals("optional")) {
						eventLog.log("WebRunner", args(
							"msg", "Element not found, skipping as 'text' command is marked 'optional'",
							"url", page.url(),
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
				break;
			}

			case WAIT: {
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
					"url", page.url(),
					"browser", commandString,
					"task", taskName,
					"element_type", elementType,
					"target", target,
					"seconds", timeoutSeconds,
					"result", Condition.ConditionResult.INFO,
					"regexp", regexp,
					"action", action
				));

				double timeoutMillis = timeoutSeconds * 1000.0;
				try {
					if (elementType.equalsIgnoreCase("contains")) {
						page.waitForURL(u -> u.contains(target), new Page.WaitForURLOptions().setTimeout(timeoutMillis));
					} else if (elementType.equalsIgnoreCase("match")) {
						Pattern urlPattern = Pattern.compile(target); // NB this takes a regexp
						page.waitForURL(u -> urlPattern.matcher(u).find(), new Page.WaitForURLOptions().setTimeout(timeoutMillis));
					} else if (!Strings.isNullOrEmpty(regexp)) {
						Pattern pattern = Pattern.compile(regexp);
						page.locator(getSelector(elementType, target))
							.filter(new Locator.FilterOptions().setHasText(pattern))
							.first()
							.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED).setTimeout(timeoutMillis));
						if (updateImagePlaceHolder || updateImagePlaceHolderOptional) {
							// make a snapshot of the page available to the test log
							browserControl.updatePlaceholder(visit.placeholder(), page.content(), "text/html", regexp, updateImagePlaceHolderOptional);
						}
					} else {
						page.waitForSelector(getSelector(elementType, target),
							new Page.WaitForSelectorOptions().setState(WaitForSelectorState.ATTACHED).setTimeout(timeoutMillis));
					}

					logger.debug(testId + ":\t\tDone waiting: " + commandString);

				} catch (TimeoutError timeoutException) {
					this.lastException = timeoutException.getMessage();
					throw new TestFailureException(testId, "Timed out waiting: " + command.toString());
				}
				break;
			}

			case WAIT_ELEMENT_INVISIBLE: {
				int timeoutSeconds = OIDFJSON.getInt(command.get(3));
				try {
					page.waitForSelector(getSelector(elementType, target),
						new Page.WaitForSelectorOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(timeoutSeconds * 1000.0));
					logger.debug(testId + ":\t\tElement with " + elementType + " '" + target + "' is now invisible");
				} catch (TimeoutError timeoutException) {
					this.lastException = timeoutException.getMessage();
					throw new TestFailureException(testId, "Timed out waiting for element to become invisible: " + command.toString());
				}
				break;
			}

			case WAIT_ELEMENT_VISIBLE: {
				int timeoutSeconds = OIDFJSON.getInt(command.get(3));
				try {
					page.waitForSelector(getSelector(elementType, target),
						new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(timeoutSeconds * 1000.0));
					logger.debug(testId + ":\t\tElement with " + elementType + " '" + target + "' is now visible");
				} catch (TimeoutError timeoutException) {
					this.lastException = timeoutException.getMessage();
					throw new TestFailureException(testId, "Timed out waiting for element visibility: " + command.toString());
				}
				break;
			}

			default:
				this.lastException = "Invalid Command " + commandString;
				throw new TestFailureException(testId, "Invalid Command: " + commandString);
		}
	}

	private Command parseCommand(String commandString) {
		if (Strings.isNullOrEmpty(commandString)) {
			// can't have a blank command
			this.lastException = "Invalid Command " + commandString;
			throw new TestFailureException(testId, "Invalid Command: " + commandString);
		}
		try {
			return Command.valueOf(commandString.toUpperCase(Locale.ROOT).replace('-', '_'));
		} catch (IllegalArgumentException e) {
			this.lastException = "Invalid Command " + commandString;
			throw new TestFailureException(testId, "Invalid Command: " + commandString);
		}
	}

	/**
	 * Returns the Playwright selector for the given selector type and value.
	 * Currently, supports id, name, xpath, css (css selector), and class (html class)
	 *
	 * @throws TestFailureException if an invalid type is specified.
	 */
	private String getSelector(String type, String value) {
		if (type.equalsIgnoreCase("id")) {
			return "id=" + value;
		} else if (type.equalsIgnoreCase("name")) {
			return "css=[name=\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"]";
		} else if (type.equalsIgnoreCase("xpath")) {
			return "xpath=" + value;
		} else if (type.equalsIgnoreCase("css")) {
			return "css=" + value;
		} else if (type.equalsIgnoreCase("class")) {
			return "css=." + value;
		}
		this.lastException = "Invalid Command Selector: Type: " + type + " Value: " + value;
		throw new TestFailureException(testId, "Invalid Command Selector: Type: " + type + " Value: " + value);
	}

	/**
	 * A locator for the first element matching the selector, i.e. Selenium's findElement() semantics.
	 */
	private Locator getLocator(String type, String value) {
		return page.locator(getSelector(type, value)).first();
	}

	/**
	 * Builds a page containing a form with the given (url-encoded) parameters as hidden fields that
	 * submits itself to the given url once loaded.
	 */
	static String buildAutoSubmittingForm(String action, String urlEncodedParams) {
		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html><html><body onload=\"setTimeout(function(){document.forms[0].submit();},0)\">");
		html.append("<form method=\"POST\" action=\"").append(HtmlUtils.htmlEscape(action)).append("\">");
		if (!Strings.isNullOrEmpty(urlEncodedParams)) {
			for (String pair : urlEncodedParams.split("&")) {
				if (pair.isEmpty()) {
					continue;
				}
				int eq = pair.indexOf('=');
				String name = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), StandardCharsets.UTF_8);
				String value = eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
				html.append("<input type=\"hidden\" name=\"").append(HtmlUtils.htmlEscape(name))
					.append("\" value=\"").append(HtmlUtils.htmlEscape(value)).append("\">");
			}
		}
		html.append("</form></body></html>");
		return html.toString();
	}

	/**
	 * Refresh the values served by {@link #getStatus()}. Must only be called from the runner thread.
	 */
	private void updateCache() {
		if (page == null) {
			return;
		}
		try {
			cachedCurrentUrl = page.url();
			cachedScreenshot = "data:image/png;base64," + Base64.getEncoder().encodeToString(page.screenshot());
		} catch (RuntimeException e) {
			logger.warn(testId + ": Failed to capture Playwright page state", e);
		}
	}

	/**
	 * True if the throwable stems from this thread being interrupted, i.e. the runner was cancelled.
	 * Playwright wraps the InterruptedException in a PlaywrightException ("Failed to read message")
	 * without re-setting the thread's interrupt flag, so the cause chain is checked as well.
	 */
	private static boolean wasInterrupted(Throwable e) {
		if (Thread.currentThread().isInterrupted()) {
			return true;
		}
		for (Throwable t = e; t != null; t = t.getCause()) {
			if (t instanceof InterruptedException) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Stops tracing (saving the trace if configured to), hands the storage state to the
	 * {@link BrowserControl} for the next runner, and releases the browser.
	 *
	 * <p>A cancelled runner arrives here with its interrupt flag set (or an interrupt about to land),
	 * which makes any Playwright call throw. The flag is cleared for the duration of the shutdown
	 * and each step is retried once after an interrupt, so the browser and driver processes are
	 * really released rather than leaked; the flag is restored afterwards.
	 */
	private void closeBrowser() {
		boolean interrupted = Thread.interrupted();
		try {
			if (context != null) {
				if (settings.traceMode() != TraceMode.OFF) {
					closeQuietly("tracing", this::stopTracing);
				}
				closeQuietly("storage state", () -> browserControl.setPlaywrightStorageState(context.storageState()));
			}
			if (page != null) {
				closeQuietly("page", page::close);
			}
			if (context != null) {
				closeQuietly("context", context::close);
			}
			if (browser != null) {
				closeQuietly("browser", browser::close);
			}
			if (playwright != null) {
				closeQuietly("driver", playwright::close);
			}
		} finally {
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void closeQuietly(String what, Runnable step) {
		try {
			step.run();
		} catch (RuntimeException e) {
			RuntimeException failure = e;
			if (wasInterrupted(e)) {
				// interrupted mid-shutdown: clear the flag and try once more so cleanup completes
				Thread.interrupted();
				try {
					step.run();
					return;
				} catch (RuntimeException retry) {
					failure = retry;
				}
			}
			logger.warn(testId + ": Error closing Playwright " + what, failure);
		}
	}

	private void stopTracing() {
		boolean save = settings.traceMode() == TraceMode.ALWAYS || (settings.traceMode() == TraceMode.ON_FAILURE && failed);
		if (!save) {
			context.tracing().stop();
			return;
		}
		if (Strings.isNullOrEmpty(settings.tracesDir())) {
			context.tracing().stop();
			logger.warn(testId + ": browser.playwright.tracesDir not configured, skipping trace save");
			eventLog.log("WebRunner", args(
				"msg", "Playwright trace not saved as browser.playwright.tracesDir is not configured",
				"result", Condition.ConditionResult.WARNING));
			return;
		}

		try {
			Path tracesDirPath = Path.of(settings.tracesDir());
			Files.createDirectories(tracesDirPath);

			Path tracePath = tracesDirPath.resolve(testId + ".zip");
			context.tracing().stop(new Tracing.StopOptions().setPath(tracePath));

			long traceSize = Files.size(tracePath);
			logger.info(testId + ": Playwright trace saved to " + tracePath + ", size: " + traceSize + " bytes");

			eventLog.log("WebRunner", args(
				"msg", "Playwright trace captured; download it from /api/log/" + testId + "/trace",
				"trace_path", tracePath.toString(),
				"trace_size", traceSize,
				"result", Condition.ConditionResult.INFO));
		} catch (IOException e) {
			logger.error(testId + ": Failed to save Playwright trace", e);
			eventLog.log("WebRunner", args(
				"msg", "Failed to save Playwright trace: " + e.getMessage(),
				"result", Condition.ConditionResult.WARNING));
		}
	}

	@Override
	public JsonObject getStatus() {
		JsonObject o = new JsonObject();
		o.addProperty("url", visit.url());
		o.addProperty("currentUrl", cachedCurrentUrl != null ? cachedCurrentUrl : visit.url());
		o.addProperty("currentScreenshot", cachedScreenshot);
		o.addProperty("currentTask", currentTask);
		o.addProperty("currentCommand", currentCommand);
		o.addProperty("lastException", lastException);
		return o;
	}

	enum Command {
		CLICK,
		TEXT,
		WAIT,
		WAIT_ELEMENT_VISIBLE,
		WAIT_ELEMENT_INVISIBLE
	}

	/** When to write a Playwright trace archive for a runner. */
	enum TraceMode {
		OFF,
		ALWAYS,
		ON_FAILURE;

		static TraceMode parse(String value) {
			switch (value.toLowerCase(Locale.ROOT)) {
				case "true":
				case "always":
					return ALWAYS;
				case "on-failure":
					return ON_FAILURE;
				default:
					return OFF;
			}
		}
	}

	/**
	 * Engine settings, read once per runner from these system properties:
	 * <ul>
	 * <li>{@code browser.playwright.type} – chromium (default), firefox or webkit</li>
	 * <li>{@code browser.playwright.headless} – default true</li>
	 * <li>{@code browser.playwright.slowMo} – milliseconds to slow every action down by, default 0</li>
	 * <li>{@code browser.playwright.extraHttpHeaders} – JSON object of headers to add to every request</li>
	 * <li>{@code browser.playwright.traceEnabled} – false (default), true or on-failure</li>
	 * <li>{@code browser.playwright.tracesDir} – directory the trace archives are written to as {@code <testId>.zip}</li>
	 * </ul>
	 */
	record Settings(String browserType, boolean headless, int slowMo, Map<String, String> extraHttpHeaders,
					TraceMode traceMode, String tracesDir) {

		static Settings fromSystemProperties() {
			return new Settings(
				System.getProperty("browser.playwright.type", "chromium").toLowerCase(Locale.ROOT),
				Boolean.parseBoolean(System.getProperty("browser.playwright.headless", "true")),
				Integer.parseInt(System.getProperty("browser.playwright.slowMo", "0")),
				parseExtraHttpHeaders(System.getProperty("browser.playwright.extraHttpHeaders", "")),
				TraceMode.parse(System.getProperty("browser.playwright.traceEnabled", "false")),
				System.getProperty("browser.playwright.tracesDir", ""));
		}

		private static Map<String, String> parseExtraHttpHeaders(String json) {
			if (Strings.isNullOrEmpty(json)) {
				return Collections.emptyMap();
			}
			try {
				Map<String, String> headers = new HashMap<>();
				JsonObject object = JsonParser.parseString(json).getAsJsonObject();
				for (String key : object.keySet()) {
					headers.put(key, OIDFJSON.getString(object.get(key)));
				}
				return Collections.unmodifiableMap(headers);
			} catch (RuntimeException e) {
				logger.warn("Ignoring browser.playwright.extraHttpHeaders, not a JSON object of strings: " + e.getMessage());
				return Collections.emptyMap();
			}
		}
	}
}
