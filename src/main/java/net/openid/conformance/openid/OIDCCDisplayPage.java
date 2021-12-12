package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddDisplayPageToAuthorizationEndpointRequest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_display_page
@PublishTestModule(
	testName = "oidcc-display-page",
	displayName = "OIDCC: display=page test",
	summary = "This test includes the display=page parameter in the request to the authorization endpoint. Use of this parameter in the request must not cause an error at the OP as per 'Mandatory to Implement Features for All OpenID Providers' in the OpenID Connect Core specification. To make sure you get a login page, please remove any cookies you may have received from the OpenID Provider before proceeding. You should get a normal user agent login page view.",
	profile = "OIDCC"
)
public class OIDCCDisplayPage extends AbstractOIDCCServerTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		return super.createAuthorizationRequestSequence()
			.then(condition(AddDisplayPageToAuthorizationEndpointRequest.class).requirements("OIDCC-3.1.2.1", "OIDCC-15.1"));
	}

}
