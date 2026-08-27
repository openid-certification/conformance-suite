package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddCdrSharingDurationClaimZeroToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.EnsureNoRefreshTokenInTokenResponse;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-cdr-sharing-duration-zero",
	displayName = "FAPI2-Security-Profile-Final: CDR test that a sharing_duration of zero results in no refresh token",
	summary = "This test requests authorisation with a sharing_duration of zero, which the CDR standards define as once off access; the Data Holder must issue only an access token, without a refresh token, on successful authorisation.",
	profile = "FAPI2-Security-Profile-Final"
)
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "plain_fapi", "openbanking_brazil", "connectid_au", "cbuae", "ksa", "fapi_client_credentials_grant", "vci", "vci_haip" })
public class FAPI2SPFinalCdrEnsureSharingDurationZeroGivesNoRefreshToken extends AbstractFAPI2SPFinalServerTestModule {

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps(boolean usePkce) {
		// added after the CDR profile setup steps, so this overwrites the sharing_duration those add
		return super.makeCreateAuthorizationRequestSteps(usePkce)
			.then(condition(AddCdrSharingDurationClaimZeroToAuthorizationEndpointRequest.class)
				.requirements("CDR-request-object"));
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		callAndContinueOnFailure(EnsureNoRefreshTokenInTokenResponse.class, Condition.ConditionResult.FAILURE, "CDR-request-object");
		fireTestFinished();
	}
}
