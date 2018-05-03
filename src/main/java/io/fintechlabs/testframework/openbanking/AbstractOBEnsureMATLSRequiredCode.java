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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointExpectingError;
import io.fintechlabs.testframework.condition.client.EnsureServerConfigurationSupportsMTLS;
import io.fintechlabs.testframework.condition.client.RemoveMTLSCertificates;
import io.fintechlabs.testframework.condition.client.SetTLSTestHostToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.SetTLSTestHostToRegistrationEndpoint;
import io.fintechlabs.testframework.condition.client.SetTLSTestHostToTokenEndpoint;
import io.fintechlabs.testframework.condition.client.SetTLSTestHostToUserInfoEndpoint;
import io.fintechlabs.testframework.condition.common.DisallowInsecureCipher;
import io.fintechlabs.testframework.condition.common.DisallowTLS10;
import io.fintechlabs.testframework.condition.common.DisallowTLS11;
import io.fintechlabs.testframework.condition.common.EnsureTLS12;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;

public abstract class AbstractOBEnsureMATLSRequiredCode extends AbstractOBServerTestModuleCode {

	public static Logger logger = LoggerFactory.getLogger(AbstractOBEnsureMATLSRequiredCode.class);

	public AbstractOBEnsureMATLSRequiredCode(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		super(id, owner, eventLog, browser, testInfo);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#start()
	 */
	@Override
	public void start() {
		setStatus(Status.RUNNING);

		// check that all known endpoints support TLS correctly

		JsonObject serverConfig = env.get("server"); // verified present by CheckServerConfiguration

		if (serverConfig.has("authorization_endpoint")) {

			callAndStopOnFailure(SetTLSTestHostToAuthorizationEndpoint.class);

			call(EnsureTLS12.class, ConditionResult.FAILURE, "FAPI-1-7.1-1");
			call(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-1-7.1-1");
			call(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-1-7.1-1");

			// Additional ciphers are allowed for the authorization endpoint
		}

		if (serverConfig.has("token_endpoint")) {

			callAndStopOnFailure(SetTLSTestHostToTokenEndpoint.class);

			call(EnsureTLS12.class, ConditionResult.FAILURE, "FAPI-1-7.1-1");
			call(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-1-7.1-1");
			call(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-1-7.1-1");

			call(DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-2-8.5-1");
		}

		if (serverConfig.has("userinfo_endpoint")) {

			callAndStopOnFailure(SetTLSTestHostToUserInfoEndpoint.class);

			call(EnsureTLS12.class, ConditionResult.FAILURE, "FAPI-1-7.1-1");
			call(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-1-7.1-1");
			call(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-1-7.1-1");

			call(DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-2-8.5-1");
		}

		if (serverConfig.has("userinfo_endpoint")) {

			callAndStopOnFailure(SetTLSTestHostToRegistrationEndpoint.class);

			call(EnsureTLS12.class, ConditionResult.FAILURE, "FAPI-1-7.1-1");
			call(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-1-7.1-1");
			call(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-1-7.1-1");

			call(DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-2-8.5-1");
		}

		performAuthorizationFlow();
	}

	@Override
	protected Object performPostAuthorizationFlow() {

		// call the token endpoint and expect an error, since this request does not
		// meet any of the OB requirements for client authentication

		createAuthorizationCodeRequest();

		callAndStopOnFailure(RemoveMTLSCertificates.class);

		callAndStopOnFailure(CallTokenEndpointExpectingError.class, "OB-5.2.2");

		fireTestFinished();
		stop();

		return redirectToLogDetailPage();
	}

}
