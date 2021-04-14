package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddRedirectUriQuerySuffix;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerTokenExpectingError;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.condition.client.RedirectQueryTestDisabled;

public abstract class AbstractFAPIRWID2MultipleClient extends AbstractFAPIRWID2ServerTestModule {

	@Override
	protected void onPostAuthorizationFlowComplete() {
		if (!isSecondClient()) {
			// Try the second client
			performAuthorizationFlowWithSecondClient();
		} else {
			switchToClient1AndTryClient2AccessToken();
			fireTestFinished();
		}
	}

	protected void performAuthorizationFlowWithSecondClient() {
		whichClient = 2;

		eventLog.startBlock(currentClientString() + "Setup");

		switchToSecondClient();

		Integer redirectQueryDisabled = env.getInteger("config", "disableRedirectQueryTest");

		if (redirectQueryDisabled != null && redirectQueryDisabled.intValue() != 0) {
			/* Temporary change to allow banks to disable tests until they have had a chance to register new
			 * clients with the new redirect uris.
			 */
			callAndContinueOnFailure(RedirectQueryTestDisabled.class, Condition.ConditionResult.FAILURE, "RFC6749-3.1.2");
		} else {
			callAndStopOnFailure(AddRedirectUriQuerySuffix.class, "RFC6749-3.1.2");
		}
		callAndStopOnFailure(CreateRedirectUri.class, "RFC6749-3.1.2");

		//exposeEnvString("client_id");

		performAuthorizationFlow();
	}

	protected void switchToClient1AndTryClient2AccessToken() {
		// Switch back to client 1
		eventLog.startBlock("Try Client1's MTLS client certificate with Client2's access token");
		unmapClient();

		callAndContinueOnFailure(CallProtectedResourceWithBearerTokenExpectingError.class, Condition.ConditionResult.FAILURE, "FAPIRW-5.2.2-5", "MTLS-3");

		eventLog.endBlock();
	}

}
