package net.openid.conformance.fapi2spfinal;

import com.google.common.base.Strings;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddCdrArrangementIdClaimToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CDRRefreshTokenRequiredWhenSharingDurationRequested;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs4xx;
import net.openid.conformance.condition.client.ExtractCdrArrangementIdFromTokenResponse;
import net.openid.conformance.condition.client.ExtractRefreshTokenFromTokenResponse;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.RefreshTokenRequestExpectingErrorSteps;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-cdr-arrangement-amendment",
	displayName = "FAPI2-Security-Profile-Final: CDR test that amending an arrangement revokes the previous tokens",
	summary = "This test authorises normally, then authorises a second time passing the cdr_arrangement_id from the first consent in the request object to amend the existing arrangement. Once the new consent is established and new tokens have been issued, the CDR standards require the Data Holder to have revoked the tokens from the original consent, so using the original refresh token and access token must then fail.",
	profile = "FAPI2-Security-Profile-Final"
)
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "plain_fapi", "openbanking_brazil", "connectid_au", "cbuae", "openbanking_chile", "ksa", "fapi_client_credentials_grant", "vci", "vci_haip" })
public class FAPI2SPFinalCdrArrangementAmendmentRevokesOldTokens extends AbstractFAPI2SPFinalServerTestModule {

	private boolean amendingArrangement = false;

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps(boolean usePkce) {
		ConditionSequence seq = super.makeCreateAuthorizationRequestSteps(usePkce);
		if (amendingArrangement) {
			seq = seq.then(condition(AddCdrArrangementIdClaimToAuthorizationEndpointRequest.class)
				.requirements("CDR-request-object"));
		}
		return seq;
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		if (!amendingArrangement) {
			eventLog.startBlock("Capture arrangement details from the first consent");
			callAndContinueOnFailure(ExtractRefreshTokenFromTokenResponse.class, Condition.ConditionResult.INFO);
			if (Strings.isNullOrEmpty(env.getString("refresh_token"))) {
				// this will always fail & stop
				callAndStopOnFailure(CDRRefreshTokenRequiredWhenSharingDurationRequested.class, "CDR-requesting-sharing-duration");
			}
			callAndStopOnFailure(ExtractCdrArrangementIdFromTokenResponse.class, "CDR-tokens");
			env.putString("first_consent_refresh_token", env.getString("refresh_token"));
			env.putObject("first_consent_access_token", env.getObject("access_token").deepCopy());
			eventLog.endBlock();

			amendingArrangement = true;
			eventLog.startBlock("Authorise again, amending the existing arrangement by passing cdr_arrangement_id");
			performAuthorizationFlow();
		} else {
			eventLog.startBlock("Attempt to use the refresh token from before the amendment - it must have been revoked");
			env.putString("refresh_token", env.getString("first_consent_refresh_token"));
			call(new RefreshTokenRequestExpectingErrorSteps(isSecondClient(), addClientAuthentication, isDpop()));
			eventLog.endBlock();

			eventLog.startBlock("Attempt to use the access token from before the amendment - it must have been revoked");
			env.putObject("access_token", env.getObject("first_consent_access_token"));
			updateResourceRequest();
			if (isDpop()) {
				updateResourceRequestAndCallProtectedResourceUsingDpop("CDR-request-object");
			} else {
				callAndStopOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE, "CDR-request-object");
			}
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs4xx.class, Condition.ConditionResult.FAILURE, "CDR-request-object");
			call(exec().unmapKey("endpoint_response"));
			eventLog.endBlock();

			fireTestFinished();
		}
	}
}
