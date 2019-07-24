package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.as.AddInvalidCHashValueToIdToken;
import io.fintechlabs.testframework.condition.as.ForceIdTokenToBeSignedWithRS256;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-rw-id2-client-test-with-mtls-holder-of-key-invalid-chash",
	displayName = "FAPI-RW-ID2: client test - invalid c_hash in id_token from authorization_endpoint, should be rejected (with MTLS)",
	summary = "This test should end with the client displaying an error message that the c_hash value in the id_token from the authorization_endpoint does not match the c_hash value in the request object",
	profile = "FAPI-RW-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
	},
	notApplicableForVariants = {
		FAPIRWID2ClientTest.variant_privatekeyjwt,
		FAPIRWID2ClientTest.variant_openbankinguk_mtls,
		FAPIRWID2ClientTest.variant_openbankinguk_privatekeyjwt
	}
)

public class FAPIRWID2ClientTestWithMTLSHolderOfKeyInvalidCHash extends AbstractFAPIRWID2ClientExpectNothingAfterAuthorisationEndpoint {

	@Variant(name = variant_mtls)
	public void setupMTLS() {
		super.setupMTLS();
	}

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddInvalidCHashValueToIdToken.class, "OIDCC-3.3.2-10");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new ConditionError(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with an invalid c_hash value from the authorization_endpoint.");
	}

}
