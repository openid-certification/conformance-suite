package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.as.RemoveSHashFromIdToken;
import io.fintechlabs.testframework.fapi.FAPIRWID2ClientTest;
import io.fintechlabs.testframework.condition.as.LogEndTestIfStateIsNotSupplied;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-rw-id2-ob-client-test-with-private-key-jwt-and-mtls-holder-of-key-invalid-missing-shash",
	displayName = "FAPI-RW-ID2-OB: client test - missing s_hash value in id_token from authorization_endpoint, should be rejected (with private_key_jwt and MTLS)",
	summary = "This test should end with the client displaying an error message that the s_hash value in the id_token from the authorization_endpoint is missing",
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
		FAPIRWID2ClientTest.variant_privatekeyjwt,
		FAPIRWID2ClientTest.variant_openbankinguk_mtls
	}
)

public class FAPIRWID2OBClientTestWithPrivateKeyJWTAndMTLSHolderOfKeyInvalidMissingSHash extends AbstractFAPIRWID2OBClientPrivateKeyExpectNothingAfterAuthorisationEndpoint {

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

		callAndStopOnFailure(RemoveSHashFromIdToken.class, "FAPI-RW-5.2.3");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new ConditionError(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with a missing s_hash value from the authorization_endpoint.");
	}

}
