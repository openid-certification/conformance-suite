package net.openid.conformance.fapi2baselineid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddRedirectUriQuerySuffix;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs4xx;
import net.openid.conformance.condition.client.RedirectQueryTestDisabled;
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.VariantConfigurationFields;

@VariantConfigurationFields(parameter = FAPI2ID2OPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client2.org_jwks"
})
public abstract class AbstractFAPI2BaselineID2MultipleClient extends AbstractFAPI2BaselineID2ServerTestModule {

	@Override
	protected void configureClient() {
		super.configureClient();
		configureSecondClient();
	}

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

		performAuthorizationFlow();
	}

	protected void switchToClient1AndTryClient2AccessToken() {
		// Switch back to client 1
		unmapClient();

		if (isDpop) {
			eventLog.startBlock("Try Client1's DPoP key with Client2's access token");
			updateResourceRequest();
		} else {
			eventLog.startBlock("Try Client1's MTLS client certificate with Client2's access token");
		}

		// As per https://datatracker.ietf.org/doc/html/rfc8705#section-3 :
		//   If they do not match, the resource access attempt MUST
		//   be rejected with an error, per [RFC6750], using an HTTP 401 status
		//   code and the "invalid_token" error code.
		// We are somewhat more permissive; historically we permitted any 4xx or 5xx code,
		// and we are not checking the WWW-Authenticate header at all
		callAndStopOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE, "FAPIRW-5.2.2-5", "RFC8705-3");
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs4xx.class, Condition.ConditionResult.FAILURE, "RFC6749-4.1.2", "RFC6750-3.1", "RFC8705-3");
		call(exec().unmapKey("endpoint_response"));

		eventLog.endBlock();
	}

}
