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
 * @author jricher
 *
 */
@PublishTestPlan (
	testPlanName = "fapi-r-code-id-token-with-pkce-test-plan",
	displayName = "FAPI-R: code id_token with PKCE Test Plan",
	profile = "FAPI-R",
	testModuleNames = {
		"fapi-r-code-id-token-with-pkce",
		"fapi-r-ensure-redirect-uri-in-authorization-request",
		"fapi-r-ensure-redirect-uri-is-registered",
		"fapi-r-require-pkce",
		"fapi-r-reject-plain-pkce"
	}
)
public class FAPI_R_CodeIdTokenWithPKCETestPlan implements TestPlan {

}
