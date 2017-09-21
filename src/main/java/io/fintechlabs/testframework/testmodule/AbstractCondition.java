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

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.logging.EventLog;

/**
 * @author jricher
 *
 */
public abstract class AbstractCondition implements Condition {

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#getMessage()
	 */
	@Override
	public String getMessage() {
		return this.getClass().getSimpleName();
	}

	
	protected void logEvent(EventLog log, String source, JsonObject event) {
		event.addProperty("condition", getMessage());
		log.log(source, event);
	}
	
}
