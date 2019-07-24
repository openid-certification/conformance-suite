package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.as.AddPrivateKeyJWTToServerConfiguration;
import io.fintechlabs.testframework.condition.as.EnsureClientAssertionTypeIsJwt;
import io.fintechlabs.testframework.condition.as.ExtractClientAssertion;
import io.fintechlabs.testframework.condition.as.ValidateClientAssertionClaims;
import io.fintechlabs.testframework.condition.as.ValidateClientSigningKeySize;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key",
	displayName = "FAPI-RW-ID2: client test (with private_key_jwt and MTLS)",
	summary = "Successful test case scenario where response_type used is code id_token combined with private_key_jwt and MTLS encryption",
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
		FAPIRWID2ClientTest.variant_mtls,
		FAPIRWID2ClientTest.variant_openbankinguk_mtls,
		FAPIRWID2ClientTest.variant_openbankinguk_privatekeyjwt
	}
)

public class FAPIRWID2ClientTestWithPrivateKeyJWTAndMTLSHolderOfKey extends FAPIRWID2ClientTest {

	@Variant(name = variant_privatekeyjwt)
	public void setupPrivateKeyJwt() {
		super.setupPrivateKeyJwt();
	}

	@Override
	protected void addTokenEndpointAuthMethodSupported() {

		callAndStopOnFailure(AddPrivateKeyJWTToServerConfiguration.class);
	}

	@Override
	protected void validateClientAuthentication() {

	callAndStopOnFailure(ExtractClientAssertion.class, "RFC7523-2.2");

	callAndStopOnFailure(EnsureClientAssertionTypeIsJwt.class, "RFC7523-2.2");

	callAndStopOnFailure(ValidateClientAssertionClaims.class, "RFC7523-3");

	callAndStopOnFailure(ValidateClientSigningKeySize.class,"FAPI-R-5.2.2.5");

	}

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

}
