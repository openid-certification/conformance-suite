package net.openid.conformance.frontchannel;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.RequestOptions;
import com.microsoft.playwright.options.WaitForSelectorState;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.info.ImageService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.runner.TestExecutionManager;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.PatternMatchUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class PlaywrightBrowserControl extends BrowserControl {

	/*
	 * EXAMPLE OF WHAT TO ADD TO CONFIG:
	 * "browser": [
	 * {
	 * "match":"https://mitreid.org/authorize*",
	 * "tasks": [
	 * {
	 * "task": "Initial Login",
	 * "match": "https://mitreid.org/login*",
	 * "commands": [
	 * ["text","id","j_username","user"],
	 * ["text","id","j_password","password"],
	 * ["click","name","submit"]
	 * ]
	 * },
	 * {
	 * "task": "Authorize Client",
	 * "match": "https://mitreid.org/authorize*",
	 * "optional": true,
	 * "commands": [
	 * ["click","id","remember-not"],
	 * ["click","name","authorize"],
	 * ["wait", "contains", "localhost", 10] // wait for up to 10 seconds for the
	 * URL to contain 'localhost' via a javascript location change, etc.
	 * ]
	 * },
	 * {
	 * "task": "Verify Complete",
	 * "match": "https://localhost*"
	 * }
	 * ]
	 * }
	 * ]
	 *
	 * Each "Task" should be things that happen on a single page. In the above
	 * example, the first task logs in and ends
	 * with clicking the submit button on the login page, resulting in a new page to
	 * get loaded. (The result of logging in).
	 *
	 * The second task clicks the "Do not remember this choice" radio button, and
	 * then clicks the authorize button which
	 * then should trigger the redirect from the server.
	 */

	private static final Logger logger = LoggerFactory.getLogger(BrowserControl.class);

	private TestExecutionManager executionManager;
	private JsonArray browserCommands = null;
	private boolean verboseLogging;

	private ImageService imageService;
	private TestInstanceEventLog eventLog;
	private Playwright playwright;

	public PlaywrightBrowserControl(JsonObject config, String testId, TestInstanceEventLog eventLog,
						  TestExecutionManager executionManager, ImageService imageService) {
		super(testId);
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

		// Initialize Playwright
		playwright = Playwright.create();
	}

	private class PlaywrightRunner extends WebRunner {

		private Browser browser;
		private Page page;
		private Response lastResponse;

		protected PlaywrightRunner(String url, JsonArray tasks, String placeholder, String method) {
			super(url, tasks, placeholder, method);
		}

		@Override
		public JsonObject toJsonObject() {
			JsonObject o = new JsonObject();
			o.addProperty("url", url);
			o.addProperty("currentUrl", getCurrentUrl());
			o.addProperty("currentTask", currentTask);
			o.addProperty("currentCommand", currentCommand);
			o.addProperty("lastResponseCode", getResponseCode());
			o.addProperty("lastResponseContentType", getResponseContentType());
			o.addProperty("lastResponseContent", getResponseContent());
			o.addProperty("lastException", lastException);
			return o;
		}

		@Override
		public String call() {
			try {
				logger.info(testId + ": Sending BrowserControl to: " + url);

				// Create browser context
				BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
					.setHeadless(true)
					.setSlowMo(100);

				browser = playwright.chromium().launch(launchOptions);
				BrowserContext context = browser.newContext(new Browser.NewContextOptions()
					.setIgnoreHTTPSErrors(true)); // Ignore certificate errors to ease local tests

				page = context.newPage();

				if (verboseLogging) {
					page.onRequest(request -> eventLog.log("Playwright", args(
						"msg", "Request (" + Integer.toString(request.hashCode(), 36) + ") " + request.method() + " " + request.url(),
						"headers", request.headers(),
						"result", Condition.ConditionResult.INFO)));

					page.onResponse(response -> eventLog.log("Playwright", args(
						"msg", "Response (" + (response.request() == null ? "N/A" : Integer.toString(response.request().hashCode(), 36)) + ") " + response.status() + " " + response.statusText(),
						"headers", response.headers(),
						"result", Condition.ConditionResult.INFO)));
				}

				eventLog.log("Playwright", args(
					"msg", "Scripted browser HTTP request",
					"http", "request",
					"request_uri", url,
					"request_method", method,
					"browser", "goToUrl"));

				if (Objects.equals(method, "POST")) {
					// Handle POST request
					java.net.URL urlObj = new java.net.URL(url);
					String params = urlObj.getQuery();

					page.context().request().post(urlObj.toString(),
						RequestOptions.create()
							.setData(params)
							.setHeader("Content-Type", "application/x-www-form-urlencoded"));

				} else {
					// Handle GET request
					lastResponse = page.navigate(url);
				}

				// Process tasks
				for (int i = 0; i < this.tasks.size(); i++) {
					boolean skip = false;
					JsonObject currentTask = this.tasks.get(i).getAsJsonObject();

					if (currentTask.get("task") == null) {
						throw new TestFailureException(testId, "Invalid Task Definition: no 'task' property");
					}

					String taskName = OIDFJSON.getString(currentTask.get("task"));

					this.currentTask = taskName;

					logger.debug(testId + ": Performing: " + taskName);
					logger.debug(testId + ": Playwright current url:" + page.url());
					// check if current URL matches the 'matcher' for the task

					String expectedUrlMatcher = "*"; // default to matching any URL
					if (currentTask.has("match")) {
						// if there is a more specific "match" element, use its value instead
						expectedUrlMatcher = OIDFJSON.getString(currentTask.get("match"));
					}

					if (!Strings.isNullOrEmpty(expectedUrlMatcher)) {
						if (!PatternMatchUtils.simpleMatch(expectedUrlMatcher, page.url())) {
							if (currentTask.has("optional") && OIDFJSON.getBoolean(currentTask.get("optional"))) {
								eventLog.log("Playwright", args(
									"msg", "Skipping optional task due to URL mismatch",
									"match", expectedUrlMatcher,
									"url", page.url(),
									"browser", "skip",
									"task", taskName,
									"commands", currentTask.get("commands")));

								skip = true;
							} else {
								eventLog.log("Playwright", args(
									"msg", "Unexpected URL for non-optional task",
									"match", expectedUrlMatcher,
									"url", page.url(),
									"result", Condition.ConditionResult.FAILURE,
									"task", taskName,
									"commands", currentTask.get("commands")));

								throw new TestFailureException(testId,
									"Playwright unexpected url for task: " + taskName);
							}
						}
					}

					if (!skip) {
						JsonArray commands = currentTask.getAsJsonArray("commands");
						if (commands != null) {

							// wait for webpage to finish loading
							try {
								page.waitForLoadState(LoadState.LOAD,
									new Page.WaitForLoadStateOptions().setTimeout(10000));
							} catch (PlaywrightException e) {
								logger.error(testId + ": Playwright caught exception: ", e);
								eventLog.log("BROWSER",
									args("msg", "Timeout waiting for page to load", "exception", e.getMessage()));
							}

							// execute all the commands in this task
							for (int j = 0; j < commands.size(); j++) {
								doCommand(commands.get(j).getAsJsonArray(), taskName);
								// clear the current command once it's done
								this.currentCommand = null;
							}
						}
						// Check the server response (Completing all browser command tasks should result
						// in a submit/new page.)

						int responseCode = lastResponse != null ? lastResponse.status() : 0;
						logger.debug(testId + ":     Response Code: " + responseCode);

						eventLog.log("Playwright", args(
							"msg", "Completed processing of webpage",
							"match", expectedUrlMatcher,
							"url", page.url(),
							"browser", "complete",
							"task", taskName,
							"result", Condition.ConditionResult.INFO,
							"response_status_code", responseCode,
							"response_status_text", lastResponse != null ? lastResponse.statusText() : ""));
					} // if we don't run the commands, just go straight to the next one
				}
				logger.debug(testId + ": Completed Browser Commands");
				// if we've successfully completed the command set, consider this URL visited
				urlVisited(url);

				return "playwright runner exited";

			} catch (Exception e) {
				logger.error(testId + ": Playwright caught exception", e);
				throw new TestFailureException(testId, "Playwright Exception: " + e.getMessage(), e);
			} finally {
				runners.remove(this);
				if (browser != null) {
					browser.close();
				}
			}
		}

		/**
		 * Given a command like '["click","id","btnId"], this will perform the WebDriver
		 * calls to execute it.
		 * Only two action types are supported this way: "click" to click on a
		 * WebElement, and "text" which enters
		 * text into a field like an input box.
		 *
		 * @param command
		 * @throws TestFailureException if an invalid command is specified
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

				try {
					eventLog.log("Playwright", args(
						"msg", "Running command (" + commandString + ")",
						"url", page.url(),
						"browser", commandString,
						"task", taskName,
						"element_type", elementType,
						"target", target,
						"result", Condition.ConditionResult.INFO));

					switch (commandString.toLowerCase()) {
						case "click":
							try {
								page.click(getSelector(elementType, target));
							} catch (Exception e) {
								String optional = command.size() >= 4 ? OIDFJSON.getString(command.get(3)) : null;
								if (optional != null && optional.equals("optional")) {
									eventLog.log("Playwright", args(
										"msg",
										"Element not found, skipping as 'click' command is marked 'optional'",
										"url", page.url(),
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
							break;
						case "text":
							// ["text", "id" or "name", "id_or_name", "text_to_enter", "optional"]

							String value = OIDFJSON.getString(command.get(3));

							eventLog.log("Playwright", args(
								"msg", "Entering text",
								"url", page.url(),
								"browser", commandString,
								"task", taskName,
								"element_type", elementType,
								"target", target,
								"value", value,
								"result", Condition.ConditionResult.INFO));

							try {

								page.fill(getSelector(elementType, target), value);
								logger.debug(testId + ":\t\tEntered text: '" + value + "' into " + target + " ("
									+ elementType + ")");
							} catch (Exception e) {
								String optional = command.size() >= 5 ? OIDFJSON.getString(command.get(4)) : null;
								if (optional != null && optional.equals("optional")) {
									eventLog.log("Playwright", args(
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
							break;

						case "wait":
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

							eventLog.log("Playwright", args(
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

							logger.debug("Waiting for selector: " + getSelector(elementType, target) + "on page: "
								+ page.url() + "element type: " + elementType + "target: " + target);

							if (elementType.equalsIgnoreCase("contains")) {
								page.waitForURL("**" + target + "**",
									new Page.WaitForURLOptions().setTimeout(timeoutSeconds * 1000));
							} else if (elementType.equalsIgnoreCase("match")) {
								page.waitForURL(Pattern.compile(target),
									new Page.WaitForURLOptions().setTimeout(timeoutSeconds * 1000));
							} else if (!Strings.isNullOrEmpty(regexp)) {
								Pattern pattern = Pattern.compile(regexp);
								page.waitForSelector(getSelector(elementType, target),
									new Page.WaitForSelectorOptions()
										.setTimeout(timeoutSeconds * 1000)
										.setState(WaitForSelectorState.VISIBLE));
								if (pattern.matcher(page.content()).find()) {
									if (updateImagePlaceHolder || updateImagePlaceHolderOptional) {
										// make a snapshot of the page available to the test log
										updatePlaceholder(this.placeholder, page.content(),
											lastResponse != null ? lastResponse.headers().get("content-type") : "",
											regexp, updateImagePlaceHolderOptional);
									}
								}
							} else {
								page.waitForSelector(getSelector(elementType, target),
									new Page.WaitForSelectorOptions()
										.setState(WaitForSelectorState.ATTACHED) // also consider hidden elements
										.setTimeout(timeoutSeconds * 1000));
							}
							logger.debug(testId + ":\t\tDone waiting: " + commandString);

							break;

						default:
							throw new TestFailureException(testId, "Invalid Command: " + commandString);
					}
				} catch (PlaywrightException e) {
					throw new TestFailureException(testId, "Command failed: " + commandString, e);
				}
			}
		}

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
					throw new TestFailureException(testId, "Invalid selector type: " + type);
			}
		}

		public String getCurrentUrl() {
			return page != null ? page.url() : "";
		}

		public int getResponseCode() {
			return lastResponse != null ? lastResponse.status() : 0;
		}

		public String getResponseContent() {
			return lastResponse != null ? lastResponse.text() : "";
		}

		public String getResponseContentType() {
			return lastResponse != null ? lastResponse.headers().get("content-type") : "";
		}
	}

	@Override
	public void goToUrl(String url, String placeholder, String method) {
		logger.debug(testId + ": goToUrl called for " + url);
		for (JsonElement commandsEl : browserCommands) {
			JsonObject commands = commandsEl.getAsJsonObject();
			String urlMatcher = OIDFJSON.getString(commands.get("match"));
			if (PatternMatchUtils.simpleMatch(urlMatcher, url)) {
				if (commands.has("match-limit")) {
					int limit = OIDFJSON.getInt(commands.get("match-limit"));
					if (limit <= 0) {
						continue;
					}
					limit--;
					commands.addProperty("match-limit", limit);
				}
				PlaywrightRunner pr = new PlaywrightRunner(url, commands.getAsJsonArray("tasks"), placeholder, method);
				executionManager.runInBackground(pr);
				runners.add(pr);
				return;
			}
		}
		urls.add(url);
		urlsWithMethod.add(new UrlWithMethod(url, method));
	}

	/**
	 * Publish the given page content to fulfill the placeholder.
	 *
	 * @param placeholder         the placeholder to fulfill
	 * @param pageSource          the source of the page as rendered
	 * @param responseContentType the content type last received from the server
	 */
	private void updatePlaceholder(String placeholder, String pageSource, String responseContentType, String regexp,
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
}
