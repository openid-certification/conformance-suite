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

package io.fintechlabs.testframework.testmodule;

/**
 * @author jricher
 *
 */
public class ConditionError extends RuntimeException {

	private static final long serialVersionUID = 6331346678545936565L;

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public ConditionError(Condition source, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(source.getMessage() + ": " + message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public ConditionError(Condition source, String message, Throwable cause) {
		super(source.getMessage() + ": "  + message, cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public ConditionError(Condition source, String message) {
		super(source.getMessage() + ": "  + message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cause
	 */
	public ConditionError(Condition source, Throwable cause) {
		super(source.getMessage() + ": "  + cause);
		// TODO Auto-generated constructor stub
	}

	
	
}
