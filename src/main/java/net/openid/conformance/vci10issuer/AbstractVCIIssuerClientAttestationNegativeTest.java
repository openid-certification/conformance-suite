package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequestOrInvalidClientAttestation;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequestOrInvalidClientAttestation;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400or401;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;

/**
 * Base class for negative tests that send an invalid client attestation (or attestation PoP)
 * and expect the authorization server to reject it.
 *
 * Where the rejection happens depends on the grant type:
 *
 * For the authorization code grant the first authenticated request is the PAR request, so the
 * invalid attestation must be rejected there — {@link #processParResponse()} validates the error
 * response and finishes the test before any redirect happens.
 *
 * For the pre-authorized code grant PAR is never used and the first authenticated request is the
 * token request, so the invalid attestation must be rejected there instead —
 * {@link #processTokenEndpointResponse()} validates the error response and finishes the test.
 * {@link #performPostAuthorizationFlow()} deliberately stops after the token endpoint call
 * (mirroring AbstractFAPI2SPFinalPerformTokenEndpoint) so the flow never continues to the
 * credential endpoint after the test has finished.
 *
 * Leaf modules only apply their specific attestation mutation, e.g. by overriding
 * {@link #afterClientAttestationGenerated()} or replacing the client authentication sequence.
 */
@VariantNotApplicable(parameter = ClientAuthType.class, values = {"mtls", "private_key_jwt"})
public abstract class AbstractVCIIssuerClientAttestationNegativeTest extends AbstractVCIIssuerTestModule {

	@Override
	protected void performPostAuthorizationFlow() {
		eventLog.startBlock(currentClientString() + "Call token endpoint");

		switch (vciGrantType) {
			case AUTHORIZATION_CODE -> createAuthorizationCodeRequest();
			case PRE_AUTHORIZATION_CODE -> createPreAuthorizationCodeRequest();
		}

		exchangeAuthorizationCode();
		// processTokenEndpointResponse() has already called fireTestFinished() — unlike the parent
		// implementation, do not continue to the credential endpoint.
	}

	@Override
	protected void processParResponse() {
		env.mapKey("endpoint_response", CallPAREndpoint.RESPONSE_KEY);
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-6.2");
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-6.2");
		callAndContinueOnFailure(CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequestOrInvalidClientAttestation.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-6.2");
		env.unmapKey("endpoint_response");

		fireTestFinished();
	}

	// Only reached for the pre-authorized code grant: for the authorization code grant
	// processParResponse() always finishes the test at the PAR endpoint. Mirrors the structure
	// used by other token-endpoint failure tests (e.g. FAPI2SPFinalEnsureClientAssertionWithNoSubFails).
	@Override
	protected void processTokenEndpointResponse() {
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB.class, Condition.ConditionResult.WARNING, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-6.2");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequestOrInvalidClientAttestation.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-6.2");

		fireTestFinished();
	}
}
