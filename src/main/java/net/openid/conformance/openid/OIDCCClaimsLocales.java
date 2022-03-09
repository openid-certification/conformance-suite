package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddClaimsLocalesSeToAuthorizationEndpointRequest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_Req_claims_locales
@PublishTestModule(
	testName = "oidcc-claims-locales",
	displayName = "OIDCC: claims_locales test",
	summary = "This test calls the authorization endpoint with claims_locale=se, which (as per section 15.1 of the OpenID Connect core spec) must at a minimum not result in errors.",
	profile = "OIDCC"
)
public class OIDCCClaimsLocales extends AbstractOIDCCServerTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		return super.createAuthorizationRequestSequence()
			.then(condition(AddClaimsLocalesSeToAuthorizationEndpointRequest.class).requirements("OIDCC-5.2", "OIDCC-15.1"));
	}

}
