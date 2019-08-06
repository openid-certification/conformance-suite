package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.as.AddInvalidSHashValueToIdToken;
import io.fintechlabs.testframework.condition.as.LogEndTestIfStateIsNotSupplied;
import io.fintechlabs.testframework.fapi.AbstractFAPIRWID2ClientExpectNothingAfterAuthorizationEndpoint;
import io.fintechlabs.testframework.fapi.FAPIRWID2ClientTest;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-rw-id2-ob-client-test-invalid-shash",
	displayName = "FAPI-RW-ID2-OB: client test - invalid s_hash in id_token from authorization_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the s_hash value in the id_token does not match the state the client sent",
	profile = "FAPI-RW-ID2-OB",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
	},
	notApplicableForVariants = {
		FAPIRWID2ClientTest.variant_mtls,
		FAPIRWID2ClientTest.variant_privatekeyjwt
	}
)

public class FAPIRWID2OBClientTestInvalidSHash extends AbstractFAPIRWID2ClientExpectNothingAfterAuthorizationEndpoint {

	@Variant(name = variant_openbankinguk_mtls)
	public void setupOpenBankingUkMTLS() {
		super.setupOpenBankingUkMTLS();
	}

	@Variant(name = variant_openbankinguk_privatekeyjwt)
	public void setupOpenBankingUkPrivateKeyJwt() {
		super.setupOpenBankingUkPrivateKeyJwt();
	}

	@Override
	protected boolean endTestIfStateIsNotSupplied() {

		String shash = env.getString("authorization_request_object", "claims.state");
		if (shash == null) {
			callAndContinueOnFailure(LogEndTestIfStateIsNotSupplied.class, ConditionResult.WARNING);
			fireTestFinished();
			return true;
		}

		return false;
	}

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddInvalidSHashValueToIdToken.class, "FAPI-RW-5.2.3");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new ConditionError(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with an invalid s_hash value from the authorization_endpoint.");
	}

}
