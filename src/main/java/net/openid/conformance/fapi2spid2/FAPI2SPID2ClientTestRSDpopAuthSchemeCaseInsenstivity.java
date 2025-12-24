package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.rs.CreateResourceEndpointDpopErrorAltSchemeCaseResponse;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VariantNotApplicable;


@PublishTestModule(
	testName = "fapi2-security-profile-id2-client-test-rs-dpop-auth-scheme-case-insensitivity",
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

@VariantNotApplicable(parameter = FAPI2SenderConstrainMethod.class, values = {
	"mtls"
})

public class FAPI2SPID2ClientTestRSDpopAuthSchemeCaseInsenstivity extends AbstractFAPI2SPID2ClientTest {

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

	@Override
	protected void createResourceEndpointDpopErrorResponse() {
		callAndContinueOnFailure(CreateResourceEndpointDpopErrorAltSchemeCaseResponse.class, Condition.ConditionResult.FAILURE);
	}
}
