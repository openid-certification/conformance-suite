package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.as.RemoveSHashFromIdToken;
import io.fintechlabs.testframework.condition.as.LogEndTestIfStateIsNotSupplied;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-rw-id2-client-test-with-mtls-holder-of-key-invalid-missing-shash",
	displayName = "FAPI-RW-ID2: client test - missing shash value in id_token from authorization_endpoint, should be rejected (with MTLS)",
	summary = "This test should end with the client displaying an error message that the shash in the id_token from the authorization_endpoint is missing",
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

public class FAPIRWID2ClientTestWithMTLSHolderOfKeyInvalidMissingSHash extends AbstractFAPIRWID2ClientExpectNothingAfterAuthorisationEndpoint {

	@Variant(name = variant_mtls)
	public void setupMTLS() {
		super.setupMTLS();
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
