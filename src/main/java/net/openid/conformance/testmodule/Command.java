package net.openid.conformance.testmodule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A builder class for test execution controls, such as mapping and unmapping keys in the environment,
 * starting and stopping blocks in the logs, and other controls not directly related to calling a
 * condition.
 */
public class Command implements TestExecutionUnit {
	private Map<String, String> mapKeys = new LinkedHashMap<>();
	private List<String> unmapKeys = new ArrayList<>();
	private List<String> removeObjects = new ArrayList<>();
	private String startBlock = null;
	private boolean endBlock = false;
	private List<String> exposeStrings = new ArrayList<>();

	/**
	 * Map a key in the environment. If this is called multiple times, the keys will be mapped in the same order they
	 * are recorded in this builder.
	 *
	 * See Environment.mapKey(String, String)
	 *
	 * @param from the key to map from (they key that will be called from the outside, any underlying object currently referenced by this key will be effectively hidden)
	 * @param to the key to map to (the key that references the underlying object that will be returned when "from" is used in the Environment)
	 * @return this builder
	 */
	public Command mapKey(String from, String to) {
		mapKeys.put(from, to);
		return this;
	}

	/**
	 * Remove a mapping on the given key. If this is called multiple times, the keys will be mapped in the same order they
	 * are recorded in this builder.
	 *
	 * See Environment.unmapKey(String)
	 *
	 * @param key the key to unmap; subsequent calls to use this key in the Environment will return the original underlying object
	 * @return this builder
	 */
	public Command unmapKey(String key) {
		unmapKeys.add(key);
		return this;
	}

	/**
	 * Remove an object from the environment. See Environment.removeObject(String)
	 */
	public Command removeObject(String key) {
		removeObjects.add(key);
		return this;
	}

	/**
	 * Start a new block in the event log, with the given message. Defaults to leaving the block state unchanged.
	 *
	 * @param msg the message to start the log block with
	 * @return this builder
	 */
	public Command startBlock(String msg) {
		this.startBlock = msg;
		return this;
	}

	/**
	 * End the current block in the event log. Defaults to false (block state is unchanged).
	 * @return
	 */
	public Command endBlock() {
		this.endBlock = true;
		return this;
	}

	/**
	 * Expose a string from the environment.
	 *
	 * @param key the environment string to expose
	 * @return this builder
	 */
	public Command exposeEnvironmentString(String key) {
		exposeStrings.add(key);
		return this;
	}

	// getters

	/**
	 * Get the set of keys to map in the Environment, in order of addition to this builder.
	 *
	 * @return A map of all keys to map in the environment as as "from -> to" sets.
	 */
	public Map<String, String> getMapKeys() {
		return mapKeys;
	}

	/**
	 * Get the list of keys to unmap in the Environment, in order of addition to this builder.
	 *
	 * @return A list of all keys to unmap.
	 */
	public List<String> getUnmapKeys() {
		return unmapKeys;
	}

	/**
	 * Get the list of objects to remove from the Environment.
	 */
	public List<String> getRemoveObjects() {
		return removeObjects;
	}

	/**
	 * Get the start block message. If no block is to be started, this returns null (default).
	 *
	 * @return the start block message, or null if no block is to be started
	 */
	public String getStartBlock() {
		return startBlock;
	}

	/**
	 * Get whether to end a block.
	 *
	 * @return true if the current block should be ended, false to leave the block state as-is (default).
	 */
	public boolean isEndBlock() {
		return endBlock;
	}

	/**
	 * Get the list of strings to expose from the environment. Defaults to an empty list.
	 *
	 * @return the list of strings to expose from the environment
	 */
	public List<String> getExposeStrings() {
		return exposeStrings;
	}

}
