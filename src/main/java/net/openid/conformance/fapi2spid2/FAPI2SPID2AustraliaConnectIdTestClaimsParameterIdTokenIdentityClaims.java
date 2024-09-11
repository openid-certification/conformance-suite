package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddEssentialTxnClaimRequestToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AustraliaConnectIdAddClaimsToAuthorizationEndpointRequestIdTokenClaims;
import net.openid.conformance.condition.client.AustraliaConnectIdCheckClaimsSupported;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates;
import net.openid.conformance.condition.client.CheckDiscEndpointClaimsParameterSupported;
import net.openid.conformance.condition.client.CheckForUnexpectedClaimsInIdToken;
import net.openid.conformance.condition.client.EnsureIdTokenContainsRequestedClaims;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-australia-connectid-test-claims-parameter-idtoken-identity-claims",
	displayName = "FAPI2-Security-Profile-ID2: test requesting id_token identity claims using the claims parameter",
	summary = "The test will request all valid ConnectID identity claims and check those contained in the server's discovery document 'claims_supported' parameter are returned in the id_token (using a variety of different forms of request), and will fail if any are not returned.\n\nThe user you use in this test must have values present for all the claims your OP supports.",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
@VariantNotApplicable(parameter = FAPI2ID2OPProfile.class, values = { "plain_fapi", "openbanking_uk", "consumerdataright_au", "openbanking_brazil", "cbuae" })
public class FAPI2SPID2AustraliaConnectIdTestClaimsParameterIdTokenIdentityClaims extends AbstractFAPI2SPID2ServerTestModule {

	@Override
	protected void performPARRedirectWithRequestUri() {
		callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates.class, "PAR-4");
		performRedirect();
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3");

		callAndContinueOnFailure(AustraliaConnectIdCheckClaimsSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3", "CID-SP-5");

		return super.makeCreateAuthorizationRequestSteps()
			.then(condition(AustraliaConnectIdAddClaimsToAuthorizationEndpointRequestIdTokenClaims.class).requirements("OIDCC-5.1", "OIDCC-5.5", "CID-SP-5"))
			.then(condition(AddEssentialTxnClaimRequestToAuthorizationEndpointRequest.class).requirements("CID-IDA-5.2-2.7"));
	}

	@Override
	protected void exchangeAuthorizationCode() {
		super.exchangeAuthorizationCode();

		callAndContinueOnFailure(EnsureIdTokenContainsRequestedClaims.class, Condition.ConditionResult.FAILURE, "OIDCC-5.5");

		callAndContinueOnFailure(AustraliaConnectIdEnsureIdTokenContainsMandatoryClaims.class, Condition.ConditionResult.FAILURE, "CID-IDA-5.1-2.6");

		// We don't include this check in the more general PerformStandardIdTokenChecks as it could be pretty noisy
		callAndContinueOnFailure(CheckForUnexpectedClaimsInIdToken.class, Condition.ConditionResult.WARNING, "OIDCC-5.1");
	}

	@Override
	protected void requestProtectedResource() {
		// not strictly necessary in this test, but also does no harm, with the advantage that it means
		// we can run this test against the rp tests (which require a userinfo call as the final step)
		super.requestProtectedResource();
	}
}
