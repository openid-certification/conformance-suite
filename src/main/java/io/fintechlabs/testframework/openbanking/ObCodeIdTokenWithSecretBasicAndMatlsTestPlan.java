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

package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

/**
 * @author ddrysdale
 *
 */
@PublishTestPlan (
	testPlanName = "ob-code-id-token-with-secret-basic-and-matls-test-plan",
	displayName = "OB: code id_token with secret basic and matls Test Plan",
	profile = "OB",
	testModuleNames = {
		"ob-discovery-end-point-verification",
		"ob-code-id-token-with-secret-basic-and-matls",
		"ob-ensure-matls-required-code-id-token-with-secret-basic-and-matls",
		"ob-ensure-matching-key-in-authorization-request-code-id-token-with-secret-basic-and-matls",
		"ob-ensure-redirect-uri-in-authorization-request-code-id-token-with-secret-basic-and-matls",
		"ob-ensure-registered-certificate-for-authorization-code-code-id-token-with-secret-basic-and-matls",
		"ob-ensure-registered-redirect-uri-code-id-token-with-secret-basic-and-matls",
		"ob-ensure-request-object-signature-algorithm-is-not-none-code-id-token-with-secret-basic-and-matls"
	}
)
public class ObCodeIdTokenWithSecretBasicAndMatlsTestPlan implements TestPlan {

}
