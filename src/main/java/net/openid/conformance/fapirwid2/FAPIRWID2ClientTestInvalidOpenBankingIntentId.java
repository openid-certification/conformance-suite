package net.openid.conformance.fapirwid2;

import net.openid.conformance.condition.as.AddInvalidOpenBankingIntentIdToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.FAPIProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-rw-id2-client-test-invalid-openbanking-intent-id",
	displayName = "FAPI-RW-ID2: client test  - invalid openbanking_intent_id, should be rejected",
	summary = "This test should end with the client displaying an error message that the openbanking_intent_id returned in id_token from authorization endpoint does not match the value sent in the request object",
	profile = "FAPI-RW-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
	}
)
@VariantNotApplicable(parameter = FAPIProfile.class, values = { "plain_fapi" })
public class FAPIRWID2ClientTestInvalidOpenBankingIntentId extends AbstractFAPIRWID2ClientExpectNothingAfterAuthorizationEndpoint {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddInvalidOpenBankingIntentIdToIdToken.class, "OBSP-3.3");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new TestFailureException(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with an invalid openbanking_intent_id value from the authorization_endpoint.");

	}

}
