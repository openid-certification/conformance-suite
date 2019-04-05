package io.fintechlabs.testframework.logging;

import java.util.Map;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.testmodule.DataUtils;

/**
 * A wrapper around an EventLog that remebers the test ID and Owner information so the caller doesn't need to
 */
public class TestInstanceEventLog implements DataUtils {

	private String testId;
	private Map<String, String> owner;
	private EventLog eventLog;

	/**
	 * @param testId
	 * @param owner
	 * @param eventLog
	 */
	public TestInstanceEventLog(String testId, Map<String, String> owner, EventLog eventLog) {
		this.testId = testId;
		this.owner = owner;
		this.eventLog = eventLog;
	}

	/**
	 * @param source
	 * @param msg
	 * @see io.fintechlabs.testframework.logging.EventLog#log(java.lang.String, java.lang.String, java.util.Map, java.lang.String)
	 */
	public void log(String source, String msg) {
		eventLog.log(testId, source, owner, msg);
	}

	/**
	 * @param source
	 * @param obj
	 * @see io.fintechlabs.testframework.logging.EventLog#log(java.lang.String, java.lang.String, java.util.Map, com.google.gson.JsonObject)
	 */
	public void log(String source, JsonObject obj) {
		eventLog.log(testId, source, owner, obj);
	}

	/**
	 * @param source
	 * @param map
	 * @see io.fintechlabs.testframework.logging.EventLog#log(java.lang.String, java.lang.String, java.util.Map, java.util.Map)
	 */
	public void log(String source, Map<String, Object> map) {
		eventLog.log(testId, source, owner, map);
	}


	public String startBlock() {
		return startBlock(null);
	}

	/**
	 * @return
	 * @see io.fintechlabs.testframework.logging.EventLog#startBlock()
	 */
	public String startBlock(String message) {
		String blockId = eventLog.startBlock();

		if (!Strings.isNullOrEmpty(message)) {
			log("-START-BLOCK-", args("msg", message, "startBlock", true));
		}

		return blockId;
	}

	/**
	 * @return
	 * @see io.fintechlabs.testframework.logging.EventLog#endBlock()
	 */
	public String endBlock() {
		return eventLog.endBlock();
	}

}
