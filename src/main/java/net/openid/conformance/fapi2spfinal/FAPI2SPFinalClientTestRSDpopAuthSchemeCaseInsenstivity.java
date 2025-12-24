package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.rs.CreateResourceEndpointDpopErrorAltSchemeCaseResponse;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VariantNotApplicable;


@PublishTestModule(
	testName = "fapi2-security-profile-final-client-test-rs-dpop-auth-scheme-case-insensitivity",
	displayName = "FAPI2-Security-Profile-Final: test client for case insensitivity of DPoP authentication scheme at the resource endpoint",
	summary = "Tests whether the client has case sensitiveness for DPoP authentication scheme when DPOP nonce error occurs at the resource endpoint",
	profile = "FAPI2-Security-Profile-Final",
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

public class FAPI2SPFinalClientTestRSDpopAuthSchemeCaseInsenstivity extends AbstractFAPI2SPFinalClientTest {

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

	@Override
	protected void createResourceEndpointDpopErrorResponse() {
		callAndContinueOnFailure(CreateResourceEndpointDpopErrorAltSchemeCaseResponse.class, Condition.ConditionResult.FAILURE);
	}
}
