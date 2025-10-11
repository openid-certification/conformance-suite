package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.client.SetAccessTokenTypeToInvertedCase;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
		testName = "fapi2-security-profile-final-access-token-type-header-case-sensitivity",
		displayName = "FAPI2-Security-Profile-Final: Test whether the resource endpoint's authorization header is case sensitive for token type",
		summary = "Tests whether the RS is case sensitive to the access token type in the token request.\n\nAs per https://www.rfc-editor.org/rfc/rfc9110#name-authentication-scheme the authentication scheme name must be treated as case insensitive.",
		profile = "FAPI2-Security-Profile-Final",
		configurationFields = {
			"server.discoveryUrl",
			"client.client_id",
			"client.scope",
			"client.jwks",
			"client2.client_id",
			"client2.scope",
			"client2.jwks",
			"resource.resourceUrl"
		}
	)
public class FAPI2SPFinalAccessTokenTypeHeaderCaseSensitivity extends AbstractFAPI2SPFinalServerTestModule {

	@Override
	protected void processTokenEndpointResponse() {
		super.processTokenEndpointResponse();
		callAndStopOnFailure(SetAccessTokenTypeToInvertedCase.class, "RFC9110-11.1");
	}

}
