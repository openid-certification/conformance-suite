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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointExpectingError;
import io.fintechlabs.testframework.condition.client.RemoveMTLSCertificates;
import io.fintechlabs.testframework.condition.common.DisallowInsecureCipher;
import io.fintechlabs.testframework.condition.common.DisallowTLS10;
import io.fintechlabs.testframework.condition.common.DisallowTLS11;
import io.fintechlabs.testframework.condition.common.EnsureTLS12;

public abstract class AbstractOBEnsureMATLSRequiredCodeIdToken extends AbstractOBServerTestModuleCodeIdToken {

	public static Logger logger = LoggerFactory.getLogger(AbstractOBEnsureMATLSRequiredCodeIdToken.class);

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#start()
	 */
	@Override
	public void start() {
		setStatus(Status.RUNNING);

		// check that all known endpoints support TLS correctly

		eventLog.startBlock("Authorization endpoint TLS test");
		env.mapKey("tls", "authorization_endpoint_tls");
		callAndContinueOnFailure(EnsureTLS12.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		callAndContinueOnFailure(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		callAndContinueOnFailure(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		// additional ciphers are allowed on the authorization endpoint

		eventLog.startBlock("Token Endpoint TLS test");
		env.mapKey("tls", "token_endpoint_tls");
		callAndContinueOnFailure(EnsureTLS12.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		callAndContinueOnFailure(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		callAndContinueOnFailure(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		callAndContinueOnFailure(DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");

		eventLog.startBlock("Userinfo Endpoint TLS test");
		env.mapKey("tls", "userinfo_endpoint_tls");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, EnsureTLS12.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");

		eventLog.startBlock("Registration Endpoint TLS test");
		env.mapKey("tls", "registration_endpoint_tls");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, EnsureTLS12.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");

		eventLog.endBlock();
		env.unmapKey("tls");

		performAuthorizationFlow();
	}

	@Override
	protected Object performPostAuthorizationFlow() {
		setStatus(Status.WAITING);

		getTestExecutionManager().runInBackground(() -> {
			setStatus(Status.RUNNING);
			// call the token endpoint and expect an error, since this request does not
			// meet any of the OB requirements for client authentication

			createAuthorizationCodeRequest();

			callAndStopOnFailure(RemoveMTLSCertificates.class);

			callAndContinueOnFailure(CallTokenEndpointExpectingError.class, ConditionResult.FAILURE, "OB-5.2.2");

			fireTestFinished();
			return "done";
		});

		return redirectToLogDetailPage();
	}

}
