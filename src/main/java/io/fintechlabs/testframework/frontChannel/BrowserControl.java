package io.fintechlabs.testframework.frontChannel;

import static io.fintechlabs.testframework.logging.EventLog.args;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.locks.Lock;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.PatternMatchUtils;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.TestFailureException;

/**
 * @author srmoore
 */
public class BrowserControl {

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
						["click","name","authorize"]
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

	private ExecutorCompletionService taskExecutor;
	private Lock lock;
	private Map<String, JsonArray> tasksForUrls = new HashMap<>();

	private List<String> urls = new ArrayList<>();
	private List<String> visited = new ArrayList<>();

	private TestInstanceEventLog eventLog;

	public BrowserControl(JsonObject config, String testId, TestInstanceEventLog eventLog, ExecutorCompletionService executorCompletionService) {
		this.testId = testId;
		this.eventLog = eventLog;

		// loop through the commands to find the various URL matchers to use
		JsonArray browserCommands = config.getAsJsonArray("browser");

		if (browserCommands == null) {
			return;
		}

		for (int bc = 0; bc < browserCommands.size(); bc++) {
			JsonObject current = browserCommands.get(bc).getAsJsonObject();
			String urlMatcher = current.get("match").getAsString();
			logger.debug("Found URL MATHCER: " + urlMatcher);
			tasksForUrls.put(urlMatcher, current.getAsJsonArray("tasks"));
		}

		this.taskExecutor = executorCompletionService;
	}

	public void goToUrl(String url) {
		// use the URL to find the command set.
		for (String urlPattern : tasksForUrls.keySet()) {
			// logger.info("Checking pattern: " +urlPattern + " against: " + url);
			// logger.info("\t" + PatternMatchUtils.simpleMatch(urlPattern,url));
			if (PatternMatchUtils.simpleMatch(urlPattern, url)) {
				// Wait till we can grab the lock before starting... then release the lock immediately
				lock.lock(); // we're only using this to make sure the test is ready to accept connections before starting
				lock.unlock();
				WebRunner wr = new WebRunner(url, tasksForUrls.get(urlPattern));
				taskExecutor.submit(wr, "web runner ran");
				logger.debug("WebRunner submitted to task executor for: " + url);
				return;
			}
		}
		logger.debug("Could not find a match for url: " + url);
		// if we couldn't find a command for this URL, leave it up to the user to do something with it
		urls.add(url);
	}

	public void urlVisited(String url) {
		logger.info("Browser went to: " + url);

		urls.remove(url);
		visited.add(url);
	}

	public void setLock(Lock lock) {
		this.lock = lock;
	}

	/**
	 * Private Runnable class that acts as the browser and allows goToUrl to return before the page gets hit.
	 * This gets handed to a {@link TaskExecutor} which manages the thread it gets run on
	 */
	private class WebRunner implements Runnable {
		private String url;
		private ResponseCodeHtmlUnitDriver driver;
		private JsonArray tasks;

		/**
		 * @param url
		 *            url to go to
		 * @param commands
		 *            {@link JsonArray} of commands to perform once we get to the page
		 */
		private WebRunner(String url, JsonArray tasks) {
			this.url = url;
			this.tasks = tasks;

			// each WebRunner gets it's own driver... that way two could run at the same time for the same test.
			this.driver = new ResponseCodeHtmlUnitDriver();
		}

		public void run() {
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
					"response_status_text", driver.getStatus()
				));
				
				
				int responseCode = driver.getResponseCode();
				
				if (responseCode != 200) {
					throw new TestFailureException(testId, "WebRunner initial GET failed with " + driver.getStatus());
				}

				for (int i = 0; i < this.tasks.size(); i++) {
					boolean skip = false;
					
					JsonObject currentTask = this.tasks.get(i).getAsJsonObject();
					
					if (currentTask.get("task") == null) {
						throw new TestFailureException(testId, "Invalid Task Definition: no 'task' property");
					}
					
					String taskName = currentTask.get("task").getAsString();
					
					logger.debug("Performing: " + taskName);
					logger.debug("WebRunner current url:" + driver.getCurrentUrl());
					// check if current URL matches the 'matcher' for the task

					String expectedUrlMatcher = "*"; // default to matching any URL
					if (currentTask.has("match")) {
						//throw new TestFailureException(testId, "Invalid Task Definition - no 'match' property - " + currentTask);
						// if there is a more specific "match" element, use its value instead
						expectedUrlMatcher = currentTask.get("match").getAsString();
					}

					if (!Strings.isNullOrEmpty(expectedUrlMatcher)) {
						if (!PatternMatchUtils.simpleMatch(expectedUrlMatcher, driver.getCurrentUrl())) {
							if (currentTask.has("optional") && currentTask.get("optional").getAsBoolean()) {
								//logStatus("Skiping Task due to URL mis-match", currentTask.get("task").getAsString(), null, currentTask);
								
								eventLog.log("WebRunner", args(
									"msg", "Skipping optional task due to URL mismatch",
									"match", expectedUrlMatcher,
									"url", driver.getCurrentUrl(),
									"browser", "skip",
									"task", taskName,
									"commands", currentTask.get("commands")
								));
								
								skip = true;
							} else {
								//logStatus("Unexpected URL for task '" + driver.getCurrentUrl() + "'", currentTask.get("task").getAsString(), ConditionResult.FAILURE, currentTask);
								
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
							}
						}

						// Check the server response (Completing all browser command tasks should result in a submit/new page.)

						responseCode = driver.getResponseCode();
						logger.debug("\tResponse Code: " + responseCode);

						if (responseCode == 200) {
							
							//logStatus(currentTask.get("task").getAsString(), ConditionResult.SUCCESS, currentTask);
							
							
							eventLog.log("WebRunner", args(
								"msg", "Completed processing of webpage",
								"match", expectedUrlMatcher,
								"url", driver.getCurrentUrl(),
								"browser", "complete",
								"task", taskName,
								"result", ConditionResult.SUCCESS,
								"response_status_code", driver.getResponseCode(),
								"response_status_text", driver.getStatus()
							));
							
						} else {
							//logStatus(currentTask.get("task").getAsString(), ConditionResult.FAILURE, currentTask);
							
							eventLog.log("WebRunner", args(
								"msg", "Failure processing of webpage",
								"match", expectedUrlMatcher,
								"url", driver.getCurrentUrl(),
								"browser", "failure",
								"task", taskName,
								"result", ConditionResult.FAILURE,
								"response_status_code", driver.getResponseCode(),
								"response_status_text", driver.getStatus()
							));
							
							throw new TestFailureException(testId, "WebRunner Response Failure: '" + driver.getStatus());
						}
					} // if we don't run the commands, just go straight to the next one
				}
				logger.debug("Completed Browser Commands");
				// if we've successfully completed the command set, consider this URL visited
				urlVisited(url);
			} catch (Exception e) {
				logger.error("WebRunner caught exception", e);
				eventLog.log("WebRunner", args("msg", e.getMessage(), "result", ConditionResult.FAILURE));
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
			// ["click", "id" or "name", "id_or_name"]
			if (!Strings.isNullOrEmpty(commandString)) {
				
				// selectors common to all elements
				String elementType = command.get(1).getAsString();
				String target = command.get(2).getAsString();
				
				if (commandString.equalsIgnoreCase("click")) {
					
					
					//logCommand(null, taskName, commandString, target, null, null);
					
					eventLog.log("WebRunner", args(
						"msg", "Clicking an element",
						"url", driver.getCurrentUrl(),
						"browser", commandString,
						"task", taskName,
						"element_type", elementType,
						"target", target,
						"result", ConditionResult.SUCCESS
					));
						
					driver.findElement(getSelector(elementType, target)).click();
					
					return;
					// ["text", "id" or "name", "id_or_name", "text_to_enter"]
				} else if (commandString.equalsIgnoreCase("text")) {
					
					String value = command.get(3).getAsString();
					
					eventLog.log("WebRunner", args(
						"msg", "Entering text",
						"url", driver.getCurrentUrl(),
						"browser", commandString,
						"task", taskName,
						"element_type", elementType,
						"target", target,
						"value", value,
						"result", ConditionResult.SUCCESS
						));
					
					WebElement entryBox = driver.findElement(getSelector(elementType, target));
					
					entryBox.sendKeys(value);
					logger.debug("\t\tEntered text: " + value);

					//logCommand(null, taskName, commandString, target, value, null);
					
					return;
				}
			}
			throw new TestFailureException(testId, "Invalid Command: " + commandString);
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
			throw new TestFailureException(testId, "Invalid Command Selector: Type: " + type + " Value: " + value);
		}
		
	}

	// Allow access to the response code via the HtmlUnit instance. The driver doesn't normally have this functionality.

	/**
	 * SubClass of {@link HtmlUnitDriver} to provide access to the response code of the last page we visited
	 */
	private class ResponseCodeHtmlUnitDriver extends HtmlUnitDriver {

		public ResponseCodeHtmlUnitDriver() { super(false); }

		public int getResponseCode() {
			return this.lastPage().getWebResponse().getStatusCode();
		}

		public String getStatus() {
			String responseCodeString = this.lastPage().getWebResponse().getStatusCode() + "-" +
				this.lastPage().getWebResponse().getStatusMessage();
			return responseCodeString;
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
	 * Get the list of URLs that have been visited.
	 * @return
	 */
	public List<String> getVisited() {
		return visited;
	}

}
