package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.as.AddAudValueAsArrayToIdToken;
import io.fintechlabs.testframework.condition.as.AddTLSClientAuthToServerConfiguration;
import io.fintechlabs.testframework.condition.as.EnsureNoClientAssertionSentToTokenEndpoint;
import io.fintechlabs.testframework.condition.as.SignIdTokenBypassingNimbusChecks;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-rw-id2-client-test-with-mtls-holder-of-key-valid-aud-as-array",
	displayName = "FAPI-RW-ID2: client test - valid aud in id_token as data type array (with MTLS)",
	summary = "This test should be successful. The value of aud within the id_token will be represented as array with one value",
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

public class FAPIRWID2ClientTestWithMTLSHolderOfKeyValidAudAsArray extends AbstractFAPIRWID2ClientTest {

	@Variant(name = variant_mtls)
	public void setupMTLS() {
		super.setupMTLS();
	}

	@Override
	protected void addCustomValuesToIdToken(){

		callAndStopOnFailure(AddAudValueAsArrayToIdToken.class,"OIDCC-3.1.3.7-3");
	}

	@Override
	protected void addCustomSignatureOfIdToken(){

		callAndStopOnFailure(SignIdTokenBypassingNimbusChecks.class);
	}

	@Override
	protected void addTokenEndpointAuthMethodSupported() {

		callAndContinueOnFailure(AddTLSClientAuthToServerConfiguration.class);
	}

	@Override
	protected void validateClientAuthentication() {

		//Parent class has already verified the presented TLS certificate so nothing to do here.

		callAndStopOnFailure(EnsureNoClientAssertionSentToTokenEndpoint.class);

	}

}
