package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.Command;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIOpenIDConnect;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-ensure-request-object-with-64-char-nonce-success",
	displayName = "FAPI2-Security-Profile-ID2: ensure request object with 64 char nonce",
	summary = "This test passes a 64 character nonce in the request object. As per https://bitbucket.org/openid/fapi/pull-requests/476/diff, the authorization server must successfully authenticate and return the nonce correctly.",
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
@VariantNotApplicable(parameter = FAPIOpenIDConnect.class, values = { "plain_oauth" })
public class FAPI2SPID2EnsureRequestObjectWith64CharNonceSuccess extends AbstractFAPI2SPID2ServerTestModule {

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		Command cmd = new Command();
		cmd.putInteger("requested_nonce_length", 64);
		return super.makeCreateAuthorizationRequestSteps()
				.insertBefore(CreateRandomNonceValue.class, cmd);
	}
}
