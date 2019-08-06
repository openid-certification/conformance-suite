package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.as.RemoveAudFromIdToken;
import io.fintechlabs.testframework.fapi.AbstractFAPIRWID2ClientExpectNothingAfterAuthorizationEndpoint;
import io.fintechlabs.testframework.fapi.FAPIRWID2ClientTest;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-rw-id2-ob-client-test-invalid-missing-aud",
	displayName = "FAPI-RW-ID2-OB: client test - missing aud value in id_token from authorization_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the aud value in the id_token from the authorization_endpoint is missing",
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

public class FAPIRWID2OBClientTestInvalidMissingAud extends AbstractFAPIRWID2ClientExpectNothingAfterAuthorizationEndpoint {

	@Variant(name = variant_openbankinguk_mtls)
	public void setupOpenBankingUkMTLS() {
		super.setupOpenBankingUkMTLS();
	}

	@Variant(name = variant_openbankinguk_privatekeyjwt)
	public void setupOpenBankingUkPrivateKeyJwt() {
		super.setupOpenBankingUkPrivateKeyJwt();
	}

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(RemoveAudFromIdToken.class, "OIDCC-3.1.3.7-3");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new ConditionError(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with a missing aud value from the authorization_endpoint.");

	}

}
