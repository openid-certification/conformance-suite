package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.as.AddInvalidOpenBankingIntentIdToIdToken;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-ob-client-test-with-private-key-jwt-and-mtls-holder-of-key-invalid-openbanking-intent-id",
	displayName = "FAPI-RW-ID2-OB: client test (with private_key_jwt and mtls holder of key and an invalid openbanking_intent_id value)",
	profile = "FAPI-RW-ID2-OB",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
	}
)

public class FAPIRWID2OBClientTestWithPrivateKeyJWTAndMTLSHolderOfKeyInvalidOpenBankingIntentId extends AbstractFAPIRWID2OBClientPrivateKeyExpectNothingAfterAuthorisationEndpoint {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddInvalidOpenBankingIntentIdToIdToken.class, "OBSP-3.3");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new ConditionError(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with an invalid openbanking_intent_id value from the authorization_endpoint.");

	}

}
