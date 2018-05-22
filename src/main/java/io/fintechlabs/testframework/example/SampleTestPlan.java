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

package io.fintechlabs.testframework.example;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

/**
 * @author jricher
 *
 */
@PublishTestPlan (
	testPlanName = "sample-test-plan",
	displayName = "Sample Test Plan",
	testModuleNames = {
		"sample-test",
		"ensure-redirect-uri-in-authorization-request",
		"ensure-redirect-uri-is-registered"
	}
)
public class SampleTestPlan implements TestPlan {

	/**
	 * 
	 */
	public SampleTestPlan() {
	}

	@Override
	public List<String> getTestModules() {
		// TODO: this is currently fully reflective, could be ways to make this better?
		PublishTestPlan pub = getClass().getAnnotation(PublishTestPlan.class);
		if (pub != null) {
			return Arrays.asList(pub.testModuleNames());
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public String getNextTestModule(String currentTestModuleName) {
		List<String> modules = getTestModules();
		
		if (modules.contains(currentTestModuleName)) {
			int prev = modules.indexOf(currentTestModuleName);
			if (prev + 1 <= modules.size()) {
				// we're still in bounds
				return modules.get(prev + 1);
			} else {
				// we're at the last element
				return null;
			}
		} else {
			return null;
		}
	}
	
}
