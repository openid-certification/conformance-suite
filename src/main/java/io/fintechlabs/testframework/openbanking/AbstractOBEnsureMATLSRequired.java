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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointExpectingError;
import io.fintechlabs.testframework.condition.client.EnsureServerConfigurationSupportsMTLS;
import io.fintechlabs.testframework.condition.client.RemoveMTLSCertificates;
import io.fintechlabs.testframework.condition.common.DisallowInsecureCipher;
import io.fintechlabs.testframework.condition.common.DisallowTLS10;
import io.fintechlabs.testframework.condition.common.DisallowTLS11;
import io.fintechlabs.testframework.condition.common.EnsureTLS12;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;

public abstract class AbstractOBEnsureMATLSRequired extends AbstractOBServerTestModuleHybridFlow {

	public static Logger logger = LoggerFactory.getLogger(AbstractOBEnsureMATLSRequired.class);

	private static final int HTTPS_DEFAULT_PORT = 443;

	private static final List<String> AUTH_TLS_ENDPOINT_KEYS = ImmutableList.of(
			"issuer",
			"authorization_endpoint",
			"token_endpoint",
			"userinfo_endpoint",
			"registration_endpoint"
	);

	public AbstractOBEnsureMATLSRequired(String name, String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		super(name, id, owner, eventLog, browser, testInfo);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#start()
	 */
	@Override
	public void start() {
		setStatus(Status.RUNNING);

		// check that all known endpoints support TLS correctly

		Set<JsonObject> tlsHosts = new HashSet<JsonObject>();
		JsonObject authEndpoint = null;

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
				// FIXME: this will be tidied up in the forthcoming TLS-test refactor
				if (entry.getKey().equals("authorization_endpoint"))
					authEndpoint = endpoint;
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

			// FIXME: for now, run tests even if TLS1.0/1.1 or insecure ciphers are present on the server
			call(EnsureTLS12.class, ConditionResult.FAILURE, "FAPI-1-7.1-1");
			call(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-1-7.1-1");
			call(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-1-7.1-1");

			// FIXME: this will be tidied up in the forthcoming TLS-test refactor
			if (!endpoint.equals(authEndpoint)) {
				call(DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-2-8.5-1");
			}
		}

		// oauth-MTLS is not required for all OpenBanking client authentication methods
		call(EnsureServerConfigurationSupportsMTLS.class, "FAPI-2-5.2.2-6");

		performAuthorizationFlow();
	}

	@Override
	protected void performPostAuthorizationFlow() {

		// call the token endpoint and expect an error, since this request does not
		// meet any of the OB requirements for client authentication

		createAuthorizationCodeRequest();

		callAndStopOnFailure(RemoveMTLSCertificates.class);

		callAndStopOnFailure(CallTokenEndpointExpectingError.class, "OB-5.2.2");
	}

}
