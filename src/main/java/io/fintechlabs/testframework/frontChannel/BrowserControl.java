package io.fintechlabs.testframework.frontChannel;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.PatternMatchUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import static io.fintechlabs.testframework.logging.EventLog.args;

/**
 * @author srmoore
 */
public class BrowserControl {

	/*  EXAMPLE OF WHAT TO ADD TO CONFIG:
	 "browserCommands": [
	   	{
	   		"match":"https://mitreid.org/authorize*",
			"commandSet": [
				{
					"task": "Initial Login",
					"expectedUrl": "https://mitreid.org/login*",
					"commands": [
						["text","id","j_username","user"],
						["text","id","j_password","password"],
						["click","name","submit"]
					]
				},
				{
					"task": "Authorize Client",
					"expectedUrl": "https://mitreid.org/authorize*",
                    "skipable": true,
					"commands": [
						["click","id","remember-not"],
						["click","name","authorize"]
					]
				},
                {
                    "task": "Verify Complete",
                    "expectedUrl": "https://localhost*"
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
	private String baseUrl;

	private TaskExecutor taskExecutor;
	private Lock lock;
	private Map<String, JsonArray> commandsForUrls = new HashMap<>();

	private List<String> urls = new ArrayList<>();
	private List<String> visited = new ArrayList<>();

	private TestInstanceEventLog eventLog;

	public BrowserControl(JsonObject config, String testId, TestInstanceEventLog eventLog){
		this.testId = testId;
		this.eventLog = eventLog;


		// loop through the commandSets to find the various URL matchers to use
		JsonArray browserCommands = config.getAsJsonArray("browserCommands");

		if (browserCommands == null) {
			return;
		}

		for (int bc = 0; bc < browserCommands.size(); bc++){
			JsonObject current = browserCommands.get(bc).getAsJsonObject();
			String urlMatcher = current.get("match").getAsString();
			logger.info("Found URL MATHCER: " + urlMatcher);
			commandsForUrls.put(urlMatcher, current.getAsJsonArray("commandSet"));
		}

		// Make this autowired? Or at least passed in from the TestRunner?
		taskExecutor = new SimpleAsyncTaskExecutor();
	}

	public void goToUrl(String url) {
		// use the URL to find the command set.
		for (String urlPattern : commandsForUrls.keySet()) {
			// logger.info("Checking pattern: " +urlPattern + " against: " + url);
			// logger.info("\t" + PatternMatchUtils.simpleMatch(urlPattern,url));
			if(PatternMatchUtils.simpleMatch(urlPattern,url)){
				// Wait till we can grab the lock before starting... then release the lock immediately
				lock.lock(); // we're only using this to make sure the test is ready to accept connections before starting
				lock.unlock();
				WebRunner wr = new WebRunner(url, commandsForUrls.get(urlPattern));
				taskExecutor.execute(wr);
				logger.info("WebRunner submitted to task executor for: " + url);
				return;
			}
		}
		logger.info("Could not find a match for url: " + url);
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
		private JsonArray commandSet;


		/**
		 * @param url			url to go to
		 * @param commandSet	{@link JsonArray} of commands to perform once we get to the page
		 */
		WebRunner(String url, JsonArray commandSet){
			this.url = url;
			this.commandSet = commandSet;

			// each WebRunner gets it's own driver... that way two could run at the same time for the same test.
			this.driver = new ResponseCodeHtmlUnitDriver();
		}

		public void run() {
			try {
				logger.info("Sending Browser to: " + url);
				driver.get(url);
				int responseCode = driver.getResponseCode();
				String commandResult = "failure";
				if (responseCode == 200) {
					commandResult = "success";
				}
				logger.info("Initial Response Code: " + responseCode);
				logStatus("Initial GET", commandResult);

				if (commandResult.equals("failure")){
					throw new TestFailureException(testId, "WebRunner initial GET failed with " + driver.getStatus());
				}

				for (int i = 0; i < this.commandSet.size(); i++) {
					boolean skipCommandSet = false;
					JsonObject currentTask = this.commandSet.get(i).getAsJsonObject();
					if(currentTask.get("task") == null) {
						throw new TestFailureException(testId, "Invalid Task Definition - no 'task' property - " + currentTask);
					}
					logger.info("Performing: " + currentTask.get("task").getAsString());
					logger.info("WebRunner current url:" + driver.getCurrentUrl());
					// check if current URL matches the 'matcher' for the task

					if(currentTask.get("expectedUrl") == null){
						throw new TestFailureException(testId, "Invalid Task Definition - no 'expectedUrl' property - " + currentTask);
					}

					String expectedUrlMatcher = currentTask.get("expectedUrl").getAsString();
					if (!Strings.isNullOrEmpty(expectedUrlMatcher)) {
						if(!PatternMatchUtils.simpleMatch(expectedUrlMatcher,driver.getCurrentUrl())){
							if(currentTask.get("skipable") != null && currentTask.get("skipable").getAsBoolean()) {
								commandResult = "";
								logStatus("Skiping Task due to URL mis-match", currentTask.get("task").getAsString(), commandResult, currentTask);
								skipCommandSet = true;
							} else {
								commandResult = "failure";
								logStatus("Unexpected URL for task '" + driver.getCurrentUrl() + "'", currentTask.get("task").getAsString(), commandResult, currentTask);
								throw new TestFailureException(testId, "WebRunner unexpected url for task: " + currentTask.get("task").getAsString());
							}
						}

					}

					// if it does run the commands
					if(!skipCommandSet) {
						JsonArray commands = currentTask.getAsJsonArray("commands");
						if (commands != null) {  // we can have no commands to just do a check that currentUrl is what we expect
							for (int j = 0; j < commands.size(); j++) {
								doCommand(commands.get(j).getAsJsonArray());
							}
						}

						// Check the server response (All browser command tasks should result in a submit/new page.)

						responseCode = driver.getResponseCode();
						logger.info("\tResponse Code: " + responseCode);

						if (responseCode == 200) {
							commandResult = "success";
							logStatus(currentTask.get("task").getAsString(), commandResult, currentTask);

						} else {
							commandResult = "failure";
							logStatus(currentTask.get("task").getAsString(), commandResult, currentTask);
							throw new TestFailureException(testId, "WebRunner Response Failure: '" + driver.getStatus());
						}
					}
				}
				logger.info("Completed Browser Commands");
				// if we've successfully completed the command set, consider this URL visited
				urlVisited(url);
			} catch (Exception e) {
				logger.error("WebRunner caught exception", e);
				eventLog.log("WebRunner", args("msg", e.getMessage(), "result", "interrupted"));
				throw new TestFailureException(testId, "Web Runner Exception: " + e.getMessage());
			}
		}

		/**
		 * Given a command like '["click","id","btnId"], this will perform the WebDriver calls to execute it.
		 * Only two action types are supported this way: "click" to click on a WebElement, and "text" which enters
		 * text into a field like an input box.
		 *
		 * @throws TestFailureException if an invalid command is specified
		 * @param command
		 */
		void doCommand(JsonArray command){
			// general format for command is [command_string, element_id_type, element_id, other_args]
			String commandString = command.get(0).getAsString();
			// ["click", "id" or "name", "id_or_name"]
			if(!Strings.isNullOrEmpty(commandString)) {
				if (commandString.equalsIgnoreCase("click")) {
					driver.findElement(getSelector(command.get(1).getAsString(), command.get(2).getAsString())).click();
					return;
					// ["text", "id" or "name", "id_or_name", "text_to_enter"]
				} else if (commandString.equalsIgnoreCase("text")) {
					WebElement entryBox = driver.findElement(getSelector(command.get(1).getAsString(), command.get(2).getAsString()));
					entryBox.sendKeys(command.get(3).getAsString());
					logger.info("\t\tEntered text: " + command.get(3).getAsString());
					return;
				}
			}
			throw new TestFailureException(testId, "Invalid Command: " + commandString);
		}

		/**
		 * Returns the appropriate {@link By} statement based on type and value.
		 * Currently supports id, name, xpath, css (css selector), and class (html class)
		 *
		 * @throws TestFailureException if an invalid type is specified.
		 * @param type
		 * @param value
		 * @return
		 */
		private By getSelector(String type, String value) {
			if (type.equalsIgnoreCase("id")){
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

		private void logStatus(String taskName, String result) {
			logStatus("", taskName, result,null);
		}

		private void logStatus(String taskName, String result, JsonObject task) {
			logStatus("", taskName, result, task);
		}

		private void logStatus(String message, String taskName, String result) {
			logStatus(message, taskName, result,null);
		}

		private void logStatus(String message, String taskName, String result, JsonObject task) {
			String logMessage = taskName + ": " + driver.getStatus();
			if (!Strings.isNullOrEmpty(message)) {
				logMessage = logMessage + " - " + message;
			}
			Map<String, Object> logArgs = args("msg", logMessage,
				"taskUrl", this.url,
				"taskName", taskName,
				"currentUrl", driver.getCurrentUrl(),
				"serverResponse", driver.getStatus(),
				"serverResponseCode", driver.getResponseCode(),
				"result", result);
			if (task != null) {
				logArgs.put("task", task);
			}
			eventLog.log(
				"WebRunner",
				logArgs
			);
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
