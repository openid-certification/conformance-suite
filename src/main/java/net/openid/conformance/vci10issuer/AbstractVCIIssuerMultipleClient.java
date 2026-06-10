package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddRedirectUriQuerySuffix;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs4xx;
import net.openid.conformance.condition.client.RedirectQueryTestDisabled;
import net.openid.conformance.vci10issuer.condition.VCIEnsureCredentialTimeClaimsNotLinkable;
import net.openid.conformance.variant.ConfigurationFields;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@ConfigurationFields({"client2.client_id", "client2.jwks"})
@VariantHidesConfigurationFields(parameter = ClientAuthType.class, value = "client_attestation",
	configurationFields = {"client2.jwks"})
public abstract class AbstractVCIIssuerMultipleClient extends AbstractVCIIssuerTestModule {

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
			// Both credentials have now been obtained (one per client). Check their time information
			// does not enable linkability (RFC 9901 §10.1) — SD-JWT iat or mdoc MSO signed. The check
			// skips automatically when fewer than two credentials were captured or when the two carry
			// different datasets. WARNING, not FAILURE: §10.1's randomize-or-round MUST is scoped to
			// "each credential in the batch", and two single issuances to two clients are not a batch.
			eventLog.startBlock("Check the two credentials' time information does not enable linkability");
			callAndContinueOnFailure(VCIEnsureCredentialTimeClaimsNotLinkable.class, Condition.ConditionResult.WARNING, "SDJWT-10.1");
			eventLog.endBlock();

			switchToClient1AndTryClient2AccessToken();
			fireTestFinished();
		}
	}

	protected void performAuthorizationFlowWithSecondClient() {
		whichClient = 2;

		eventLog.startBlock(currentClientString() + "Setup");

		switchToSecondClient();

		if (clientAuthType == ClientAuthType.CLIENT_ATTESTATION) {
			generateClientAttestationKeys();
		}

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

		if (isDpop()) {
			eventLog.startBlock("Try Client1's DPoP key with Client2's access token");
		} else {
			eventLog.startBlock("Try Client1's MTLS client certificate with Client2's access token");
		}

		// As per https://datatracker.ietf.org/doc/html/rfc8705#section-3 :
		//   If they do not match, the resource access attempt MUST
		//   be rejected with an error, per [RFC6750], using an HTTP 401 status
		//   code and the "invalid_token" error code.
		// We are somewhat more permissive; historically we permitted any 4xx or 5xx code,
		// and we are not checking the WWW-Authenticate header at all
		if(isDpop()) {
			updateResourceRequestAndCallProtectedResourceUsingDpop("FAPIRW-5.2.2-5", "RFC8705-3");
		} else {
			callAndStopOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE, "FAPIRW-5.2.2-5", "RFC8705-3");
		}
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs4xx.class, Condition.ConditionResult.FAILURE, "RFC6749-4.1.2", "RFC6750-3.1", "RFC8705-3");
		call(exec().unmapKey("endpoint_response"));

		eventLog.endBlock();
	}

}
