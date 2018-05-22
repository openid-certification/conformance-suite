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

package io.fintechlabs.testframework.info;

import java.util.Map;

import com.google.gson.JsonObject;
import com.mongodb.DBObject;

import io.fintechlabs.testframework.plan.TestPlan;

/**
 * @author jricher
 *
 */
public interface TestPlanService {

	/**
	 * @param planId
	 * @param testName
	 * @param id
	 */
	void updateTestPlanWithModule(String planId, String testName, String id);

	/**
	 * @param id
	 * @param planName
	 * @param config
	 * @param owner
	 * @param testModules
	 */
	void createTestPlan(String id, String planName, JsonObject config, String[] testModules);

	/**
	 * @param id
	 * @return
	 */
	Map getTestPlan(String id);

}
