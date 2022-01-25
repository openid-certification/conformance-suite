package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddUiLocalesFromConfigurationToAuthorizationEndpointRequest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_Req_ui_locales
@PublishTestModule(
	testName = "oidcc-ui-locales",
	displayName = "OIDCC: ui_locales test",
	summary = "This test includes the ui_locales parameter in the request to the authorization endpoint, with the value set to that provided in the configuration (or 'se' if no value probably). Use of this parameter in the request must not cause an error at the OP. Please remove any cookies you may have received from the OpenID Provider before proceeding. You need to do this so you can check that the login page is displayed using one of the requested locales.",
	profile = "OIDCC"
)
public class OIDCCUiLocales extends AbstractOIDCCServerTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		return super.createAuthorizationRequestSequence()
			.then(condition(AddUiLocalesFromConfigurationToAuthorizationEndpointRequest.class).requirements("OIDCC-3.1.2.1"));
	}

}
