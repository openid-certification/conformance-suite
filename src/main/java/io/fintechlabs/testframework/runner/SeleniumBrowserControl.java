package io.fintechlabs.testframework.runner;

import com.gargoylesoftware.htmlunit.Page;
import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.PatternMatchUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;

/**
 * @author srmoore
 */
public class SeleniumBrowserControl implements BrowserControl {

	/*  EXAMPLE OF WHAT TO ADD TO CONFIG:
	 "browserCommands": [
	   	{
	   		"match":"https://mitreid.org/authorize*",
			"commandSet": [
				{
					"task": "Initial Login",
					"commands": [
						["text","id","j_username","user"],
						["text","id","j_password","password"],
						["click","name","submit"]
					]
				},
				{
					"task": "Authorize Client",
					"commands": [
						["click","id","remember-not"],
						["click","name","authorize"]
					]
				}
			]
		}
     ]

     Each "Task" should be things that happen on a single page. In the above example, the first task logs in and ends
     with clicking the submit button on the login page, resulting in a new page to get loaded. (The result of logging in).

     The second task clicks the "Do not remember this choice" radio button, and then clicks the authorize button which
     then should trigger the redirect from the server.
	 */

	private static Logger logger = LoggerFactory.getLogger(SeleniumBrowserControl.class);

	private String testId;

	private TaskExecutor taskExecutor;
	private Lock lock;
	private Map<String, JsonArray> commandsForUrls;

	SeleniumBrowserControl(JsonObject config, String testId){
		this.testId = testId;


		// loop through the commandSets to find the various URL matchers to use
		JsonArray browserCommands = config.getAsJsonArray("browserCommands");
		commandsForUrls = new HashMap<>();
		for (int bc = 0; bc < browserCommands.size(); bc++){
			JsonObject current = browserCommands.get(bc).getAsJsonObject();
			String urlMatcher = current.get("match").getAsString();
			logger.info("Found URL MATHCER: " + urlMatcher);
			commandsForUrls.put(urlMatcher, current.getAsJsonArray("commandSet"));
		}

		// quite down CSS errors/warnings in the parser selenium/htmlunit uses
		java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);

		// Make this autowired? Or at least passed in from the TestRunner?
		taskExecutor = new SimpleAsyncTaskExecutor();
	}

	@Override
	public void goToUrl(String url) {
		// use the URL to find the command set. If there isn't a match, throw an error?
		for (String urlPattern : commandsForUrls.keySet()) {
			logger.info("Checking pattern: " +urlPattern + " against: " + url);
			logger.info("\t" + PatternMatchUtils.simpleMatch(urlPattern,url));
			if(PatternMatchUtils.simpleMatch(urlPattern,url)){
				WebRunner wr = new WebRunner(url, commandsForUrls.get(urlPattern), lock);
				taskExecutor.execute(wr);
				logger.info("We ran the WebRunner... we should return now");
				return;
			}
		}
		logger.info("Could not find a match for this url");
		throw new TestFailureException(testId, "Could not fetch commands for URL: " + url);
	}

	@Override
	public void urlVisited(String url) {
		// SHOULD NEVER BE CALLED
	}

	@Override
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
		private Lock lock;

		/**
		 * @param url			url to go to
		 * @param commandSet	{@link JsonArray} of commands to perform once we get to the page
		 */
		WebRunner(String url, JsonArray commandSet, Lock lock){
			this.url = url;
			this.commandSet = commandSet;
			this.lock = lock;

			// each WebRunner gets it's own driver... that way two could run at the same time for the same test.
			this.driver = new ResponseCodeHtmlUnitDriver();
		}

		public void run() {
			this.lock.lock();
			try {
				logger.info("Sending Browser to: " + url);
				driver.get(url);
				int responseCode = driver.getResponseCode();
				logger.info("Inital Response Code: " + responseCode);

				//JsonArray commandSet = config.getAsJsonArray("browserCommands");

				for (int i = 0; i < this.commandSet.size(); i++) {
					JsonObject currentTask = this.commandSet.get(i).getAsJsonObject();
					logger.info("Performing: " + currentTask.get("task").getAsString());
					JsonArray commands = currentTask.getAsJsonArray("commands");
					for (int j = 0; j < commands.size(); j++) {
						doCommand(commands.get(j).getAsJsonArray());
					}
					responseCode = driver.getResponseCode();
					logger.info("\tResponse Code: " + responseCode);
				}
				logger.info("Completed Browser Commands");
			} finally {
				this.lock.unlock();
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

	}

	// Allow access to the response code via the HtmlUnit instance. The driver doesn't normally have this functionality.

	/**
	 * SubClass of {@link HtmlUnitDriver} to provide access to the response code of the last page we visited
	 */
	private class ResponseCodeHtmlUnitDriver extends HtmlUnitDriver {

		public ResponseCodeHtmlUnitDriver() { super(false); }

		public int getResponseCode() {
			Page page = this.lastPage();
			return page.getWebResponse().getStatusCode();
		}

	}
}
