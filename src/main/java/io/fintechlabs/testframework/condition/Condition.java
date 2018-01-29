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

package io.fintechlabs.testframework.condition;

import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public interface Condition {

	/**
	 * Tests if the condition holds true. Reads from the given environment and returns a potentially modified environment.
	 * 
	 * Throws ConditionError when condition isn't met.
	 * 
	 * Decorate with @PreEnvironment to ensure objects or strings are in the environment before evaluation.
	 * Decorate with @PostEnvironment to ensure objects or strings are in the environment after evaluation.
	 */
	Environment evaluate(Environment env);

	public static enum ConditionResult {
		FAILURE,
		WARNING,
		INFO,
		SUCCESS,
		REVIEW
	}

	/**
	 * @return a a string suitable for tagging this as a "source" in the logs, defaults to the class name
	 */
	default public String getMessage() {
		return this.getClass().getSimpleName();
	}

}
