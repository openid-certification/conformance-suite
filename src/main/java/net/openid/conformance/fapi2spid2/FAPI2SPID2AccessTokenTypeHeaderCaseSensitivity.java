package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.client.SetTokenRequestTokenTypeToInvertedCase;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
		testName = "fapi2-security-profile-id2-access-token-type-header-case-sensitivity",
		displayName = "FAPI2-Security-Profile-ID2: Test whether the Token endpoint's authorization header is case sensitive for token type",
		summary = "Tests whether the AS is case sensitive to the access token type in the token request",
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
public class FAPI2SPID2AccessTokenTypeHeaderCaseSensitivity extends AbstractFAPI2SPID2ServerTestModule {

	@Override
	protected void exchangeAuthorizationCode() {
		super.exchangeAuthorizationCode();
		callAndContinueOnFailure(SetTokenRequestTokenTypeToInvertedCase.class);
	}

}
