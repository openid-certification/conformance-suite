package net.openid.conformance.fapi;

import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.as.AddInvalidAudValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-rw-id2-client-test-invalid-aud",
	displayName = "FAPI-RW-ID2: client test - invalid aud in id_token from authorization_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the aud value in the id_token does not match the client id",
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

public class FAPIRWID2ClientTestInvalidAud extends AbstractFAPIRWID2ClientExpectNothingAfterAuthorizationEndpoint {

	@Variant(name = variant_mtls)
	public void setupMTLS() {
		super.setupMTLS();
	}

	@Variant(name = variant_privatekeyjwt)
	public void setupPrivateKeyJwt() {
		super.setupPrivateKeyJwt();
	}

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

		callAndStopOnFailure(AddInvalidAudValueToIdToken.class, "OIDCC-3.1.3.7-3");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new ConditionError(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with an invalid aud value from the authorization_endpoint.");
	}

}
