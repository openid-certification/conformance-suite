package io.fintechlabs.testframework.runner;

import com.gargoylesoftware.htmlunit.Page;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;

import java.util.logging.Level;

/**
 * @author srmoore
 */
public class SeleniumBrowserControl implements BrowserControl {

	private static Logger logger = LoggerFactory.getLogger(SeleniumBrowserControl.class);

	private JsonObject config;
	private ResponseCodeHtmlUnitDriver driver;

	private TaskExecutor taskExecutor;

	public SeleniumBrowserControl(JsonObject config){
		this.config = config;
		this.driver = new ResponseCodeHtmlUnitDriver();

		// quite down CSS errors/warnings in the parser selenium/htmlunit uses
		java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);

		// Make this autowired? Or at least passed in from the TestRunner?
		taskExecutor = new SimpleAsyncTaskExecutor();
	}

	private class webRunner implements Runnable {
		private String url;
		private ResponseCodeHtmlUnitDriver driver;

		public webRunner(String url, ResponseCodeHtmlUnitDriver driver){
			this.url = url;
			this.driver = driver;
		}

		// TODO: Make this take a series of browser commands, as well as better logging messages.
		public void run() {
			// FIXME: We shouldn't be 'sleeping' here. Some better scheduling/execution should be used.
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			logger.info("Sending Browser to: " + url);
			driver.get(url);
			int responseCode = driver.getResponseCode();
			logger.info("Inital Response Code: " + responseCode);

			JsonArray commandSet = config.getAsJsonArray("browserCommands");

			for (int i = 0; i < commandSet.size(); i++) {
				JsonObject currentTask = commandSet.get(i).getAsJsonObject();
				logger.info("Performing: " + currentTask.get("task").getAsString());
				JsonArray commands = currentTask.getAsJsonArray("commands");
				for (int j = 0; j < commands.size(); j++){
					doCommand(commands.get(j).getAsJsonArray());
				}
				responseCode = driver.getResponseCode();
				logger.info("\tResponse Code: " + responseCode);
			}
			logger.info("Completed Browser Commands");
		}

		// TODO fill this out
		public void doCommand(JsonArray commandSet){
			// general format for command is [command_string, element_id_type, element_id, other_args]

			// ["click", "id" or "name", "id_or_name"]
			if (commandSet.get(0).getAsString().equalsIgnoreCase("click")) {
				if(commandSet.get(1).getAsString().equalsIgnoreCase("id")){
					driver.findElement(By.id(commandSet.get(2).getAsString())).click();
					logger.info("\t\tClicking on element with ID: " + commandSet.get(2).getAsString());
				} else if (commandSet.get(1).getAsString().equalsIgnoreCase("name")){
					driver.findElement(By.name(commandSet.get(2).getAsString())).click();
					logger.info("\t\tClicking on element with NAME: " + commandSet.get(2).getAsString());
				}
			// ["text", "id" or "name", "id_or_name", "text_to_enter"]
			} else if(commandSet.get(0).getAsString().equalsIgnoreCase("text")){
				WebElement entryBox = null;
				if(commandSet.get(1).getAsString().equalsIgnoreCase("id")){
					entryBox = driver.findElement(By.id(commandSet.get(2).getAsString()));
					logger.info("\t\tFound input with ID: " + commandSet.get(2).getAsString());
				} else if (commandSet.get(1).getAsString().equalsIgnoreCase("name")){
					entryBox = driver.findElement(By.name(commandSet.get(2).getAsString()));
					logger.info("\t\tFound input with NAME: " + commandSet.get(2).getAsString());
				}
				entryBox.sendKeys(commandSet.get(3).getAsString());
				logger.info("\t\tEntered text: " + commandSet.get(3).getAsString());
			}
		}

	}

	@Override
	public void goToUrl(String url) {
		webRunner wr = new webRunner(url, driver);
		taskExecutor.execute(wr);
	}

	@Override
	public void urlVisited(String url) {
		// SHOULD NEVER BE CALLED
	}

	private class ResponseCodeHtmlUnitDriver extends HtmlUnitDriver {

		public ResponseCodeHtmlUnitDriver() { super(false); }

		public int getResponseCode() {
			Page page = this.lastPage();
			return page.getWebResponse().getStatusCode();
		}

	}
}
