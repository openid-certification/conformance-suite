package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.AddUserinfoClaimsToIdTokenClaims;
import net.openid.conformance.condition.as.EnsureScopeContainsAtLeastOneOfProfileEmailPhoneAddress;
import net.openid.conformance.condition.as.FilterUserInfoForScopes;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-client-test-scope-userinfo-claims",
	displayName = "OIDCC: Relying party test. Request claims using scope values.",
	summary = "The client is expected to request claims using one of more of the following scope values: " +
		"profile, email, phone, address." +
		" If no access token is issued (when using Implicit Flow with response_type='id_token') " +
		"the ID Token contains the requested claims." +
		" Corresponds to rp-scope-userinfo-claims in the old suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClientTestScopeUserInfoClaims extends AbstractOIDCCClientTest {
	@Override
	protected void validateAuthorizationEndpointRequestParameters()
	{
		super.validateAuthorizationEndpointRequestParameters();
		callAndStopOnFailure(EnsureScopeContainsAtLeastOneOfProfileEmailPhoneAddress.class);
	}

	@Override
	protected void generateIdTokenClaims()
	{
		super.generateIdTokenClaims();
		//when no Access Token is issued (which is the case for the response_type value id_token),
		//the resulting Claims are returned in the ID Token.
		if(responseType.includesIdToken() && !responseType.includesCode() && !responseType.includesToken()) {
			callAndStopOnFailure(FilterUserInfoForScopes.class, "OIDCC-5.4");
			callAndStopOnFailure(AddUserinfoClaimsToIdTokenClaims.class, "OIDCC-5.4");
		}
	}
}
