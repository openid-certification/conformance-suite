package io.fintechlabs.testframework.frontChannel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.PatternMatchUtils;

import com.gargoylesoftware.htmlunit.DefaultPageCreator;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.info.ImageService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.runner.TestExecutionManager;
import io.fintechlabs.testframework.testmodule.DataUtils;
import io.fintechlabs.testframework.testmodule.TestFailureException;

/**
 * @author srmoore
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

	private static Logger logger = LoggerFactory.getLogger(BrowserControl.class);

	private String testId;

	private TestExecutionManager executionManager;
	private Map<String, JsonArray> tasksForUrls = new HashMap<>();

	private List<String> urls = new ArrayList<>();
	private List<String> visited = new ArrayList<>();
	private Queue<WebRunner> runners = new ConcurrentLinkedQueue<>();

	private ImageService imageService;

	private TestInstanceEventLog eventLog;

	public BrowserControl(JsonObject config, String testId, TestInstanceEventLog eventLog, TestExecutionManager executionManager, ImageService imageService) {
		this.testId = testId;
		this.eventLog = eventLog;
		this.executionManager = executionManager;
		this.imageService = imageService;

		// loop through the commands to find the various URL matchers to use
		JsonArray browserCommands = config.getAsJsonArray("browser");

		if (browserCommands == null) {
			return;
		}

		for (int bc = 0; bc < browserCommands.size(); bc++) {
			JsonObject current = browserCommands.get(bc).getAsJsonObject();
			String urlMatcher = current.get("match").getAsString();
			logger.debug("Found URL MATCHER: " + urlMatcher);
			tasksForUrls.put(urlMatcher, current.getAsJsonArray("tasks"));
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

	/**
	 * Tell the front-end control that a url needs to be visited. If there is a matching
	 * browser configuration element, this will execute automatically. If there is no
	 * matching element, the url is made available for user interaction.
	 *
	 * @param url the url to be visited
	 * @param placeholder the placeholder in the log that is expecting the results of
	 * 	the transaction, usually as a screenshot, can be null
	 */
	public void goToUrl(String url, String placeholder) {
		// use the URL to find the command set.
		for (String urlPattern : tasksForUrls.keySet()) {
			// logger.info("Checking pattern: " +urlPattern + " against: " + url);
			// logger.info("\t" + PatternMatchUtils.simpleMatch(urlPattern,url));
			if (PatternMatchUtils.simpleMatch(urlPattern, url)) {
				WebRunner wr = new WebRunner(url, tasksForUrls.get(urlPattern), placeholder);
				executionManager.runInBackground(wr);
				logger.debug("WebRunner submitted to task executor for: " + url);

				runners.add(wr);

				return;
			}
		}
		logger.debug("Could not find a match for url: " + url);
		// if we couldn't find a command for this URL, leave it up to the user to do something with it
		urls.add(url);
	}

	/**
	 * Tell the front end control that a url has been visited by the user externally.
	 *
	 * @param url the url that has been visited
	 */
	public void urlVisited(String url) {
		logger.info("Browser went to: " + url);

		urls.remove(url);
		visited.add(url);
	}

	/**
	 * Private Runnable class that acts as the browser and allows goToUrl to return before the page gets hit.
	 * This gets handed to a {@link TaskExecutor} which manages the thread it gets run on
	 */
	private class WebRunner implements Callable {
		private String url;
		private ResponseCodeHtmlUnitDriver driver;
		private JsonArray tasks;
		private String currentTask;
		private String currentCommand;
		private String lastException;
		private String placeholder;

		/**
		 * @param url
		 *            url to go to
		 * @param tasks
		 *            {@link JsonArray} of commands to perform once we get to the page
		 */
		private WebRunner(String url, JsonArray tasks, String placeholder) {
			this.url = url;
			this.tasks = tasks;
			this.placeholder = placeholder;

			// each WebRunner gets it's own driver... that way two could run at the same time for the same test.
			this.driver = new ResponseCodeHtmlUnitDriver();
		}

		@Override
		public String call() {
			try {
				logger.info("Sending BrowserControl to: " + url);

				eventLog.log("WebRunner", args(
					"msg", "Scripted browser HTTP request",
					"http", "request",
					"request_uri", url,
					"request_method", "GET",
					"browser", "goToUrl"
				));

				// do the actual HTTP GET
				driver.get(url);

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

					String taskName = currentTask.get("task").getAsString();

					this.currentTask = taskName;

					logger.debug("Performing: " + taskName);
					logger.debug("WebRunner current url:" + driver.getCurrentUrl());
					// check if current URL matches the 'matcher' for the task

					String expectedUrlMatcher = "*"; // default to matching any URL
					if (currentTask.has("match")) {
						// if there is a more specific "match" element, use its value instead
						expectedUrlMatcher = currentTask.get("match").getAsString();
					}

					if (!Strings.isNullOrEmpty(expectedUrlMatcher)) {
						if (!PatternMatchUtils.simpleMatch(expectedUrlMatcher, driver.getCurrentUrl())) {
							if (currentTask.has("optional") && currentTask.get("optional").getAsBoolean()) {
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
									"result", ConditionResult.FAILURE,
									"task", taskName,
									"commands", currentTask.get("commands")
								));

								throw new TestFailureException(testId, "WebRunner unexpected url for task: " + currentTask.get("task").getAsString());
							}
						}

					}

					// if it does run the commands
					if (!skip) {
						JsonArray commands = currentTask.getAsJsonArray("commands");
						if (commands != null) { // we can have zero commands to just do a check that currentUrl is what we expect

							// execute all of the commands in this task
							for (int j = 0; j < commands.size(); j++) {
								doCommand(commands.get(j).getAsJsonArray(), taskName);
								// clear the current command once it's done
								this.currentCommand = null;
							}
						}

						// Check the server response (Completing all browser command tasks should result in a submit/new page.)

						responseCode = driver.getResponseCode();
						logger.debug("\tResponse Code: " + responseCode);

						eventLog.log("WebRunner", args(
							"msg", "Completed processing of webpage",
							"match", expectedUrlMatcher,
							"url", driver.getCurrentUrl(),
							"browser", "complete",
							"task", taskName,
							"result", ConditionResult.INFO,
							"response_status_code", driver.getResponseCode(),
							"response_status_text", driver.getStatus()
						));
					} // if we don't run the commands, just go straight to the next one
				}
				logger.debug("Completed Browser Commands");
				// if we've successfully completed the command set, consider this URL visited
				runners.remove(this);
				urlVisited(url);

				return "web runner exited";
			} catch (Exception e) {
				logger.error("WebRunner caught exception", e);
				eventLog.log("WebRunner",
					ex(e,
						args("msg", e.getMessage(), "page_source", driver.getPageSource(),
							"content_type", driver.getResponseContentType(), "result", ConditionResult.FAILURE)));
				// note that this leaves us in the current list of runners for the executing test
				this.lastException = e.getMessage();
				throw new TestFailureException(testId, "Web Runner Exception: " + e.getMessage());
			}
		}

		/**
		 * Given a command like '["click","id","btnId"], this will perform the WebDriver calls to execute it.
		 * Only two action types are supported this way: "click" to click on a WebElement, and "text" which enters
		 * text into a field like an input box.
		 *
		 * @throws TestFailureException
		 *             if an invalid command is specified
		 * @param command
		 */
		private void doCommand(JsonArray command, String taskName) {
			// general format for command is [command_string, element_id_type, element_id, other_args]
			String commandString = command.get(0).getAsString();
			if (!Strings.isNullOrEmpty(commandString)) {

				this.currentCommand = commandString;

				// selectors common to all elements
				String elementType = command.get(1).getAsString();
				String target = command.get(2).getAsString();

				if (commandString.equalsIgnoreCase("click")) {
					// ["click", "id" or "name", "id_or_name"]

					eventLog.log("WebRunner", args(
						"msg", "Clicking an element",
						"url", driver.getCurrentUrl(),
						"browser", commandString,
						"task", taskName,
						"element_type", elementType,
						"target", target,
						"result", ConditionResult.INFO
					));

					driver.findElement(getSelector(elementType, target)).click();

					logger.debug("Clicked: " + target + " (" + elementType + ")");
				} else if (commandString.equalsIgnoreCase("text")) {
					// ["text", "id" or "name", "id_or_name", "text_to_enter"]

					String value = command.get(3).getAsString();

					eventLog.log("WebRunner", args(
						"msg", "Entering text",
						"url", driver.getCurrentUrl(),
						"browser", commandString,
						"task", taskName,
						"element_type", elementType,
						"target", target,
						"value", value,
						"result", ConditionResult.INFO
						));

					WebElement entryBox = driver.findElement(getSelector(elementType, target));

					entryBox.sendKeys(value);
					logger.debug("\t\tEntered text: '" + value + "' into " + target + " (" + elementType + ")" );

				} else if (commandString.equalsIgnoreCase("wait")) {
					// ["wait","match" or "contains", "urlmatch_or_contains_string",timeout_in_seconds]
					// 	 'wait' will wait for the URL to match a regex, or for it to contain a string, OR
					//	 'wait' can wait for the presence of an element (like a button) using the same selectors (id, name) as click and text above.
					// if waiting for an element, the next parameter can be a regexp to be matched
					// and the final parameter can be 'update-image-placeholder' to mark an image placeholder as satisfied

					int timeoutSeconds = command.get(3).getAsInt();
					String regexp = command.size() >= 5 ? command.get(4).getAsString() : null;
					String action = command.size() >= 6 ? command.get(5).getAsString() : null;
					boolean updateImagePlaceHolder = false;
					if (!Strings.isNullOrEmpty(action)) {
						if (action.equals("update-image-placeholder")) {
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
						"result", ConditionResult.INFO,
						"regexp", regexp,
						"action", action
					));

					WebDriverWait waiting = new WebDriverWait(driver, timeoutSeconds, 100); // hook to wait for this condition, check every 100 milliseconds until the max seconds
					try {
						if (elementType.equalsIgnoreCase("contains")){
							waiting.until(ExpectedConditions.urlContains(target));
						} else if (elementType.equalsIgnoreCase("match")) {
							waiting.until(ExpectedConditions.urlMatches(target)); // NB this takes a regexp
						} else if (!Strings.isNullOrEmpty(regexp)) {
							Pattern pattern = Pattern.compile(regexp);
							waiting.until(ExpectedConditions.textMatches(getSelector(elementType, target), pattern));
							if (updateImagePlaceHolder) {
								// make a snapshot of the page available to the test log
								updatePlaceholder(this.placeholder, driver.getPageSource(), driver.getResponseContentType());
							}
						} else {
							waiting.until(ExpectedConditions.presenceOfElementLocated(getSelector(elementType, target)));
						}

						logger.debug("\t\tDone waiting: " + commandString);

					} catch (TimeoutException timeoutException) {
						this.lastException = timeoutException.getMessage();
						throw new TestFailureException(testId, "Timed out waiting: " + commandString);
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
		 * Currently supports id, name, xpath, css (css selector), and class (html class)
		 *
		 * @throws TestFailureException
		 *             if an invalid type is specified.
		 * @param type
		 * @param value
		 * @return
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

	private class BrowserControlPageCreator extends DefaultPageCreator {
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

	/**
	 * SubClass of {@link HtmlUnitDriver} to provide access to the response code of the last page we visited
	 */
	private class ResponseCodeHtmlUnitDriver extends HtmlUnitDriver {

		public ResponseCodeHtmlUnitDriver() { super(true); }

		public int getResponseCode() {
			return this.lastPage().getWebResponse().getStatusCode();
		}

		public String getResponseContent() {
			return this.lastPage().getWebResponse().getContentAsString();
		}

		public String getResponseContentType() {
			return this.lastPage().getWebResponse().getContentType();
		}

		public String getStatus() {
			String responseCodeString = this.lastPage().getWebResponse().getStatusCode() + "-" +
				this.lastPage().getWebResponse().getStatusMessage();
			return responseCodeString;
		}

		@Override
		protected WebClient modifyWebClient(WebClient client) {
			client.setPageCreator(new BrowserControlPageCreator());
			return client;
		}
	}

	/**
	 * Get the list of URLs that require user interaction.
	 * @return
	 */
	public List<String> getUrls() {
		return urls;
	}

	/**
	 * Publish the given page content to fulfill the placeholder.
	 *
	 * @param placeholder the placeholder to fulfill
	 * @param pageSource the source of the page as rendered
	 * @param responseContentType the content type last received from the server
	 */
	private void updatePlaceholder(String placeholder, String pageSource, String responseContentType) {
		Map<String, Object> update = new HashMap<>();
		update.put("page_source", pageSource);
		update.put("content_type", responseContentType);

		imageService.fillPlaceholder(testId, placeholder, update, true);

		eventLog.log("BROWSER", args("msg", "Updated placeholder from scripted browser", "placeholder", placeholder));

		if (imageService.getRemainingPlaceholders(testId, true).isEmpty()) {
			// no remaining placeholders
			eventLog.log("BROWSER", args("msg", "All placeholders filled by scripted browser"));
		}
	}

	/**
	 * Get the list of URLs that have been visited.
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

}
