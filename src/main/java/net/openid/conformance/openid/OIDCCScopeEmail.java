package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddEmailScopeToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

// Corresponds to OP-scope-email
@PublishTestModule(
	testName = "oidcc-scope-email",
	displayName = "OIDCC: check email scope",
	summary = "This test requests authorization with email scope.",
	profile = "OIDCC",
	configurationFields = {
			"server.discoveryUrl",
			"client.scope",
			"client2.scope",
			"resource.resourceUrl"
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values={"id_token", "id_token token"})
public class OIDCCScopeEmail extends AbstractOIDCCServerTest {

	@Override
	protected void createAuthorizationRequest() {
		call(new CreateAuthorizationRequestSteps()
				.then(condition(AddEmailScopeToAuthorizationEndpointRequest.class)));
	}

}
