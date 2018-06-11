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

// author: ddrysdale

package io.fintechlabs.testframework.openbanking;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointClaimsParameterSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointClaimsSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointGrantTypesSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointIdTokenSigningAlgValuesSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointJwksUri;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointRegistrationEndpoint;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointRequestObjectSigningAlgValuesSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointRequestParameterSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointRequestUriParameterSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointRequireRequestUriRegistration;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointResponseTypesSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointScopesSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointTokenEndpointAuthMethodsSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointTokenEndpointAuthSigningAlgValuesSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointUserinfoSigningAlgValuesSupported;
import io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.TestModule.Status;

@PublishTestModule(
		testName = "ob-discovery-end-point-verification",
		displayName = "OB: Discovery Endpoint Verification",
		profile = "OB",
		configurationFields = {
			"server.discoveryUrl",
		}
)

public class OBDiscoveryEndpointVerification extends AbstractTestModule {

	public OBDiscoveryEndpointVerification(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		super(id, owner, eventLog, browser, testInfo);
	}
	
	@Override
	public void start() {
		
		setStatus(Status.RUNNING);
		
		call(CheckDiscEndpointClaimsParameterSupported.class, ConditionResult.FAILURE);
		call(CheckDiscEndpointClaimsSupported.class, ConditionResult.FAILURE);
		call(CheckDiscEndpointGrantTypesSupported.class, ConditionResult.FAILURE);
		call(CheckDiscEndpointResponseTypesSupported.class, ConditionResult.FAILURE);
		call(CheckDiscEndpointRequestParameterSupported.class, ConditionResult.FAILURE);
		call(CheckDiscEndpointRequestUriParameterSupported .class, ConditionResult.FAILURE);
		call(CheckDiscEndpointRequireRequestUriRegistration.class, ConditionResult.FAILURE);
		call(CheckDiscEndpointScopesSupported.class, ConditionResult.FAILURE);
		call(CheckDiscEndpointIdTokenSigningAlgValuesSupported.class, ConditionResult.FAILURE);
		call(CheckDiscEndpointRequestObjectSigningAlgValuesSupported.class, ConditionResult.FAILURE);
		call(CheckDiscEndpointTokenEndpointAuthMethodsSupported.class, ConditionResult.FAILURE);
		call(CheckDiscEndpointTokenEndpointAuthSigningAlgValuesSupported .class, ConditionResult.FAILURE);
		call(CheckDiscEndpointUserinfoSigningAlgValuesSupported .class, ConditionResult.FAILURE);
		call(CheckDiscEndpointTokenEndpoint.class, ConditionResult.FAILURE);
		call(CheckDiscEndpointAuthorizationEndpoint.class, ConditionResult.FAILURE);
		skipIfMissing(new String[] { "registration_endpoint" }, new String[] {}, ConditionResult.INFO,
				CheckDiscEndpointRegistrationEndpoint.class, ConditionResult.FAILURE, "NO_URL");
		call(CheckDiscEndpointJwksUri.class, ConditionResult.FAILURE);
		
		fireTestFinished();
		
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session,
			JsonObject requestParts) {
		throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
	}

	
	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session,
			JsonObject requestParts) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configure(JsonObject config, String baseUrl) {
		// TODO Auto-generated method stub
		env.putString("base_url", baseUrl);
		env.put("config", config);
		callAndStopOnFailure(GetDynamicServerConfiguration.class);
		setStatus(Status.CONFIGURED);
		fireSetupDone();
		start();
		
	}
	
}
