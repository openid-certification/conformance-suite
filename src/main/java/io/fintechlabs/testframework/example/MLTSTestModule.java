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

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.*;
import io.fintechlabs.testframework.condition.common.*;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.UserFacing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * @author jricher
 *
 */
@PublishTestModule(
	testName = "mtls-test",
	displayName = "mlts AS Test",
	configurationFields = {
		"mtls.cert",
		"mtls.key",
		"mtls.ca",
		
	}
)
public class MLTSTestModule extends AbstractTestModule {

	public static Logger logger = LoggerFactory.getLogger(MLTSTestModule.class);
	/**
	 *
	 */
	public MLTSTestModule(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		super(id, owner, eventLog, browser, testInfo);
	}

	/* (non-Javadoc)
	 * @see io.bspk.selenium.TestModule#configure(com.google.gson.JsonObject)
	 */
	public void configure(JsonObject config, String baseUrl) {
		env.putString("base_url", baseUrl);
		env.put("config", config);
		
		callAndStopOnFailure(ExtractMTLSCertificatesFromConfiguration.class);
		// Set up the resource endpoint configuration
		//callAndStopOnFailure(GetResourceEndpointConfiguration.class);
		
		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	/* (non-Javadoc)
	 * @see io.bspk.selenium.TestModule#start()
	 */
	public void start() {
		
		setStatus(Status.RUNNING);
		
		callAndStopOnFailure(ValidateMTLSCertificatesAsX509.class);

		setStatus(Status.FINISHED);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.TestModule#handleHttp(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, org.springframework.util.MultiValueMap, org.springframework.ui.Model)
	 */
	@Override
	public ModelAndView handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		logIncomingHttpRequest(path, requestParts);

		// dispatch based on the path
		if (path.equals("callback")) {
			return handleCallback(requestParts);
		} else {
			return new ModelAndView("testError");
		}

	}


	@UserFacing
	private ModelAndView handleCallback(JsonObject requestParts) {

		// process the callback
		setStatus(Status.RUNNING);

		env.put("callback_params", requestParts.get("params").getAsJsonObject());
		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);

		callAndStopOnFailure(CheckMatchingStateParameter.class);

		callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		callAndStopOnFailure(AddFormBasedClientSecretAuthenticationParameters.class);
		//require(CreateClientAuthenticationAssertionClaims.class);

		//require(SignClientAuthenticationAssertion.class);

		//require(AddClientAssertionToTokenEndpointRequest.class);

		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class, "FAPI-1-5.2.2-14");

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndStopOnFailure(CheckForIdTokenValue.class);

		callAndStopOnFailure(CheckForScopesInTokenResponse.class, "FAPI-1-5.2.2-15");

		callAndStopOnFailure(ParseIdToken.class, "FAPI-1-5.2.2-24");

		callAndStopOnFailure(ValidateIdToken.class, "FAPI-1-5.2.2-24");

		callAndStopOnFailure(ValidateIdTokenSignature.class, "FAPI-1-5.2.2-24");

		call(ValidateStateHash.class, ConditionResult.FAILURE, "FAPI-2-5.2.2-4");

		call(CheckForRefreshTokenValue.class);

		callAndStopOnFailure(EnsureMinimumTokenEntropy.class, "FAPI-1-5.2.2-16");

		// verify the access token against a protected resource

		/*
		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);

		callAndStopOnFailure(SetTLSTestHostToResourceEndpoint.class);

		call(DisallowInsecureCipher.class, "FAPI-2-8.5-1");

		callAndStopOnFailure(CallAccountsEndpointWithBearerToken.class, "FAPI-1-6.2.1-3");

		call(DisallowAccessTokenInQuery.class, "FAPI-1-6.2.1-4");

		callAndStopOnFailure(CheckForDateHeaderInResourceResponse.class, "FAPI-1-6.2.1-11");

		call(CheckForFAPIInteractionIdInResourceResponse.class, "FAPI-1-6.2.1-12");

		call(EnsureMatchingFAPIInteractionId.class, "FAPI-1-6.2.1-12");

		call(EnsureResourceResponseEncodingIsUTF8.class, "FAPI-1-6.2.1-9");
		*/

		fireTestFinished();
		stop();

		return new ModelAndView("complete", ImmutableMap.of("test", this));

	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#handleHttpMtls(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, com.google.gson.JsonObject)
	 */
	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		/*
		eventLog.log(getId(), getName() + " MTLS Routing", requestParts.get("headers").getAsJsonObject());
		
		return new ModelAndView("complete", ImmutableMap.of("test", this));
		*/
		throw new TestFailureException(getId(), "Got an HTTP response on a call we weren't expecting");

	}

}
