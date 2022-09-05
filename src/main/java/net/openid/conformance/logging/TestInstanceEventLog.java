package net.openid.conformance.logging;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.DataUtils;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * A wrapper around an EventLog that supports blocks and remembers the test ID and Owner information
 */
public class TestInstanceEventLog implements DataUtils {

	private String testId;
	private Map<String, String> owner;
	private EventLog eventLog;

	// a block identifier for a log entry
	private String blockId = null;

	// random number generator
	private Random random = new SecureRandom();

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
	 * @see EventLog#log(java.lang.String, java.lang.String, java.util.Map, java.lang.String)
	 */
	public synchronized void log(String source, String msg) {
		if (blockId != null) {
			eventLog.log(testId, source, owner, Map.of("blockId", blockId, "msg", msg));
		} else {
			eventLog.log(testId, source, owner, msg);
		}
	}

	/**
	 * @param source
	 * @param obj
	 * @see EventLog#log(java.lang.String, java.lang.String, java.util.Map, com.google.gson.JsonObject)
	 */
	public synchronized void log(String source, JsonObject obj) {
		JsonObject logObj;
		if (blockId != null) {
			logObj = new JsonObject();
			for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
				logObj.add(entry.getKey(), entry.getValue().deepCopy());
			}
			logObj.addProperty("blockId", blockId);
		} else {
			logObj = obj;
		}
		eventLog.log(testId, source, owner, logObj);
	}

	/**
	 * @param source
	 * @param map
	 * @see EventLog#log(java.lang.String, java.lang.String, java.util.Map, java.util.Map)
	 */
	public synchronized void log(String source, Map<String, Object> map) {
		Map<String, Object> logMap;
		if (blockId != null) {
			logMap = new HashMap<>(map);
			logMap.put("blockId", blockId);
		} else {
			logMap = map;
		}
		eventLog.log(testId, source, owner, logMap);
	}

	private String startBlock() {
		// create a random six-character hex string that we can use as a CSS color code in the logs
		blockId = Strings.padStart(
			Integer.toHexString(
				random.nextInt(256 * 256 * 256))
			, 6, '0');

		return blockId;
	}

	/**
	 * Start a new log block and return its ID
	 *
	 * @return
	 */
	public synchronized String startBlock(String message) {
		String blockId = startBlock();

		if (!Strings.isNullOrEmpty(message)) {
			log("-START-BLOCK-", args("msg", message, "startBlock", true));
		}

		return blockId;
	}

	/**
	 * end a log block and return the previous block ID
	 */
	public synchronized String endBlock() {
		String oldBlock = blockId;
		blockId = null;
		return oldBlock;
	}

}
