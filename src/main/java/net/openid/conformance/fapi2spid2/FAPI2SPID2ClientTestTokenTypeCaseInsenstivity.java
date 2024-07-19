package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.SetTokenResponseTokenTypeToInvertedCase;
import net.openid.conformance.testmodule.PublishTestModule;


@PublishTestModule(
	testName = "fapi2-security-profile-id2-client-test-token-type-case-insensitivity",
	displayName = "FAPI2-Security-Profile-ID2: test client for case insensitivity of token type in token endpoint response",
	summary = "Tests whether the client has case sensitiveness when an all inverted case token_type is returned in the token endpoint response",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"waitTimeoutSeconds"
	}
)

public class FAPI2SPID2ClientTestTokenTypeCaseInsenstivity extends AbstractFAPI2SPID2ClientTest {
	@Override
	protected void issueAccessToken() {
		super.issueAccessToken();
		callAndContinueOnFailure(SetTokenResponseTokenTypeToInvertedCase.class, Condition.ConditionResult.INFO);
	}

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}
}
