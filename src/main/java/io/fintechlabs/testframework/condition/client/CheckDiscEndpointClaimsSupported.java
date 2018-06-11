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

//Author: ddrysdale

package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckDiscEndpointClaimsSupported extends ValidateJsonArray {
	
	private static final String environmentVariable = "claims_supported";
	private static final String environmentVariableText = "Claims Supported Supported";
	  
	private static final String[] SET_VALUES = new String[] { "openbanking_intent_id" };
	private static final int minimumMatchesRequired = SET_VALUES.length;
	
	
	private static final String errorMessageWhenNull = "Endpoint Claims Supported: Not Found";
	private static final String errorMessageNonArray = "Expect Json Array of " + environmentVariableText;
	private static final String errorMessageNotEnough = "The server does not support enough of the required claims.";
  

	public CheckDiscEndpointClaimsSupported(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		
		return validate(env, environmentVariable, environmentVariableText, SET_VALUES, minimumMatchesRequired, 
				errorMessageWhenNull, errorMessageNonArray, errorMessageNotEnough);
	}	


}








