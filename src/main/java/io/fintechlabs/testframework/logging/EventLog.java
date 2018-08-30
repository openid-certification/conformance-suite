/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework.logging;

import java.util.Map;

import com.google.gson.JsonObject;

/**
 * @author jricher
 *
 */
public interface EventLog {

	/**
	 * @param testId
	 *            The instance identifier of the test
	 * @param source
	 *            The source of the event
	 * @param owner
	 *            The owner of the test run
	 * @param msg
	 *            The message to log
	 */
	void log(String testId, String source, Map<String, String> owner, String msg);

	/**
	 * @param testId
	 *            The instance identifier of the test
	 * @param source
	 *            The source of the event
	 * @param owner
	 *            The owner of the test run
	 * @param obj
	 *            The message to log
	 */
	void log(String testId, String source, Map<String, String> owner, JsonObject obj);

	/**
	 * @param testId
	 *            The instance identifier of the test
	 * @param source
	 *            The source of the event
	 * @param owner
	 *            The owner of the test run
	 * @param map
	 *            The message to log
	 */
	void log(String testId, String source, Map<String, String> owner, Map<String, Object> map);

	/**
	 * Start a new log block and return its ID
	 *
	 * @return
	 */
	String startBlock();

	/**
	 * end a log block and return the previous block ID
	 */
	String endBlock();

}
