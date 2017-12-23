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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AddClientIdToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.AddNonceToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.AddStateToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.BuildRequestObjectRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.CheckIfAuthorizationEndpointError;
import io.fintechlabs.testframework.condition.CheckMatchingStateParameter;
import io.fintechlabs.testframework.condition.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.ConvertAuthorizationEndpointRequestToRequestObject;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.CreateAuthorizationEndpointRequestFromClientInformation;
import io.fintechlabs.testframework.condition.CreateRandomImplicitSubmitUrl;
import io.fintechlabs.testframework.condition.CreateRandomNonceValue;
import io.fintechlabs.testframework.condition.CreateRandomStateValue;
import io.fintechlabs.testframework.condition.CreateRedirectUri;
import io.fintechlabs.testframework.condition.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import io.fintechlabs.testframework.condition.DisallowInsecureCipher;
import io.fintechlabs.testframework.condition.DisallowTLS10;
import io.fintechlabs.testframework.condition.DisallowTLS11;
import io.fintechlabs.testframework.condition.EnsureServerConfigurationSupportsMTLS;
import io.fintechlabs.testframework.condition.EnsureTls12;
import io.fintechlabs.testframework.condition.EnsureTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.ExtractAuthorizationCodeFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.ExtractImplicitHashToCallbackResponse;
import io.fintechlabs.testframework.condition.ExtractJWKsFromClientConfiguration;
import io.fintechlabs.testframework.condition.FetchServerKeys;
import io.fintechlabs.testframework.condition.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.GetStaticServerConfiguration;
import io.fintechlabs.testframework.condition.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;
import io.fintechlabs.testframework.condition.SignRequestObject;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.UserFacing;

public class OBEnsureMATLSRequired extends AbstractTestModule {

	public static Logger logger = LoggerFactory.getLogger(OBEnsureMATLSRequired.class);

	private static final int HTTPS_DEFAULT_PORT = 443;

	private static final List<String> AUTH_TLS_ENDPOINT_KEYS = ImmutableList.of(
			"issuer",
			"authorization_endpoint",
			"token_endpoint",
			"userinfo_endpoint",
			"registration_endpoint"
	);

	public OBEnsureMATLSRequired(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		super("ob-ensure-matls-required", id, owner, eventLog, browser, testInfo);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#configure(com.google.gson.JsonObject, java.lang.String)
	 */
	@Override
	public void configure(JsonObject config, String baseUrl) {
		env.putString("base_url", baseUrl);
		env.put("config", config);

		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Make sure we're calling the right server configuration
		call(GetDynamicServerConfiguration.class);
		call(GetStaticServerConfiguration.class);

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckServerConfiguration.class);

		// check that all known endpoints support TLS correctly

		Set<JsonObject> tlsHosts = new HashSet<JsonObject>();

		JsonObject serverConfig = env.get("server"); // verified present by CheckServerConfiguration
		for (Map.Entry<String,JsonElement> entry : serverConfig.entrySet()) {
			if (AUTH_TLS_ENDPOINT_KEYS.contains(entry.getKey())) {
				String endpointUrl = entry.getValue().getAsString();
				UriComponents components = UriComponentsBuilder.fromUriString(endpointUrl).build();

				String host = components.getHost();
				int port = components.getPort();

				if (port < 0) {
					port = HTTPS_DEFAULT_PORT;
				}

				JsonObject endpoint = new JsonObject();
				endpoint.addProperty("testHost", host);
				endpoint.addProperty("testPort", port);
				tlsHosts.add(endpoint);
			}
		}

		for (JsonObject endpoint : tlsHosts) {
			eventLog.log(getName(),
					"Testing TLS support for " +
					endpoint.get("testHost").getAsString() +
					":" + endpoint.get("testPort").getAsInt());

			env.get("config").remove("tls");
			env.get("config").add("tls", endpoint);

			callAndStopOnFailure(EnsureTls12.class, "FAPI-1-7.1-1");
			// FIXME: for now, run tests even if TLS1.0/1.1 or insecure ciphers are present on the server
			call(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-1-7.1-1");
			call(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-1-7.1-1");
			call(DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-2-8.5-1");
		}

		// oauth-MTLS is not required for all OpenBanking client authentication methods
		call(EnsureServerConfigurationSupportsMTLS.class);

		callAndStopOnFailure(FetchServerKeys.class);

		// Set up the client configuration
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		exposeEnvString("client_id");

		callAndStopOnFailure(ExtractJWKsFromClientConfiguration.class);

		// Do not extract any client certificates; we want to make sure the request fails

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#start()
	 */
	@Override
	public void start() {
		setStatus(Status.RUNNING);

		callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

		callAndStopOnFailure(CreateRandomStateValue.class);
		exposeEnvString("state");
		callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(CreateRandomNonceValue.class);
		exposeEnvString("nonce");
		callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken.class);

		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

		callAndStopOnFailure(SignRequestObject.class);

		callAndStopOnFailure(BuildRequestObjectRedirectToAuthorizationEndpoint.class);

		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), "Redirecting to url " + redirectTo);

		browser.goToUrl(redirectTo);

		setStatus(Status.WAITING);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#handleHttp(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, com.google.gson.JsonObject)
	 */
	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		eventLog.log(getName(), "Path: " + path);
		eventLog.log(getName(), "Params: " + requestParts);

		// dispatch based on the path

		// these are all user-facing and will require user-facing error pages, so we wrap them

		if (path.equals("callback")) {
			return handleCallback(requestParts);
		} else if (path.equals(env.getString("implicit_submit", "path"))) {
			return handleImplicitSubmission(requestParts);
		} else {
			return new ModelAndView("testError");
		}
	}

	@UserFacing
	private ModelAndView handleCallback(JsonObject requestParts) {
		setStatus(Status.RUNNING);

		callAndStopOnFailure(CreateRandomImplicitSubmitUrl.class);

		setStatus(Status.WAITING);

		return new ModelAndView("implicitCallback",
				ImmutableMap.of("test", this,
					"implicitSubmitUrl", env.getString("implicit_submit", "fullUrl")));
	}

	private ModelAndView handleImplicitSubmission(JsonObject requestParts) {
		// process the callback
		setStatus(Status.RUNNING);

		String hash = requestParts.get("body").getAsString();

		logger.info("Hash: " + hash);

		env.putString("implicit_hash", hash);

		callAndStopOnFailure(ExtractImplicitHashToCallbackResponse.class);

		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);

		callAndStopOnFailure(CheckMatchingStateParameter.class);

		// call the token endpoint and expect an error, since this request does not
		// meet any of the OB requirements for client authentication

		callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);

		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(EnsureTokenEndpointResponseError.class);

		setStatus(Status.FINISHED);
		fireTestSuccess();
		return new ModelAndView("complete", ImmutableMap.of("test", this));
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#handleHttpMtls(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, com.google.gson.JsonObject)
	 */
	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		throw new TestFailureException(getId(), "Unexpected HTTP call: " + path);
	}

}
