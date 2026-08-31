package net.openid.conformance.fapi2spfinal;

import com.google.common.base.Strings;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CDRRefreshTokenRequiredWhenSharingDurationRequested;
import net.openid.conformance.condition.client.CallTokenIntrospectionEndpoint;
import net.openid.conformance.condition.client.CdrCheckForUnexpectedClaimsInIntrospectionResponse;
import net.openid.conformance.condition.client.CdrEnsureIntrospectionResponseArrangementIdMatchesTokenResponse;
import net.openid.conformance.condition.client.CdrEnsureIntrospectionResponseContainsScope;
import net.openid.conformance.condition.client.CdrEnsureIntrospectionResponseDoesNotContainUsername;
import net.openid.conformance.condition.client.CdrValidateIntrospectionResponseExp;
import net.openid.conformance.condition.client.CreateIntrospectionRequestForRefreshToken;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureIntrospectionResponseActiveIsTrue;
import net.openid.conformance.condition.client.ExtractRefreshTokenFromTokenResponse;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-cdr-refresh-token-introspection-expiry",
	displayName = "FAPI2-Security-Profile-Final: CDR test that introspecting the refresh token reveals the sharing arrangement expiry",
	summary = "This test authorises with a sharing_duration of 90 days and then presents the issued refresh token to the token introspection endpoint. The CDR standards require refresh tokens to be issued with an expiry equal to the authorised sharing duration, and the introspection response exp claim to reveal that expiry, so the returned exp must be approximately 90 days in the future.",
	profile = "FAPI2-Security-Profile-Final"
)
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "plain_fapi", "openbanking_brazil", "connectid_au", "cbuae", "ksa", "fapi_client_credentials_grant", "vci", "vci_haip" })
public class FAPI2SPFinalCdrRefreshTokenIntrospectionExpiry extends AbstractFAPI2SPFinalServerTestModule {

	@Override
	protected void onPostAuthorizationFlowComplete() {
		eventLog.startBlock("Introspect the refresh token to check the sharing arrangement expiry");

		callAndContinueOnFailure(ExtractRefreshTokenFromTokenResponse.class, Condition.ConditionResult.INFO);
		if (Strings.isNullOrEmpty(env.getString("refresh_token"))) {
			// this will always fail & stop
			callAndStopOnFailure(CDRRefreshTokenRequiredWhenSharingDurationRequested.class, "CDR-requesting-sharing-duration");
		}

		callAndStopOnFailure(CreateIntrospectionRequestForRefreshToken.class, "RFC7662-2.1");

		mapClientAuthKeys("introspection_endpoint_request_form_parameters", "introspection_endpoint_request_headers");
		call(sequence(addClientAuthentication));
		unmapClientAuthKeys();

		callAndStopOnFailure(CallTokenIntrospectionEndpoint.class, "RFC7662-2");

		env.mapKey("endpoint_response", CallTokenIntrospectionEndpoint.RESPONSE_KEY);
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "RFC7662-2.2");
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "RFC7662-2.2");
		env.unmapKey("endpoint_response");

		callAndContinueOnFailure(EnsureIntrospectionResponseActiveIsTrue.class, Condition.ConditionResult.FAILURE, "RFC7662-2.2");
		callAndContinueOnFailure(CdrValidateIntrospectionResponseExp.class, Condition.ConditionResult.FAILURE, "CDR-requesting-sharing-duration", "CDR-tokens");
		callAndContinueOnFailure(CdrEnsureIntrospectionResponseContainsScope.class, Condition.ConditionResult.FAILURE, "CDR-security-endpoints");
		callAndContinueOnFailure(CdrEnsureIntrospectionResponseArrangementIdMatchesTokenResponse.class, Condition.ConditionResult.FAILURE, "CDR-security-endpoints");
		callAndContinueOnFailure(CdrEnsureIntrospectionResponseDoesNotContainUsername.class, Condition.ConditionResult.FAILURE, "CDR-security-endpoints");
		callAndContinueOnFailure(CdrCheckForUnexpectedClaimsInIntrospectionResponse.class, Condition.ConditionResult.WARNING, "RFC7662-2.2");

		eventLog.endBlock();
		fireTestFinished();
	}
}
