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

package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

/**
 * @author jheenan
 *
 */
@PublishTestPlan (
	testPlanName = "fapi-code-id-token-with-mtls-test-plan",
	displayName = "FAPI: code id_token with mtls Test Plan",
	profile = "FAPI",
	testModuleNames = {
		"code-id-token-with-mtls",
		"ensure-redirect-uri-in-authorization-request",
		"ensure-redirect-uri-is-registered",
		"ensure-request-object-signature-algorithm-is-not-null",
		"reject-code-flow-test"
	}
)
public class FAPICodeIdTokenWithMtlsTestPlan implements TestPlan {

}
