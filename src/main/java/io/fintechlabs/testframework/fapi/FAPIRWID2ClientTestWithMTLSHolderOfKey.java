package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.as.AddTLSClientAuthToServerConfiguration;
import io.fintechlabs.testframework.condition.as.EnsureNoClientAssertionSentToTokenEndpoint;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-client-test-with-mtls-holder-of-key",
	displayName = "FAPI-RW-ID2: client test",
	summary = "Successful test case scenario where response_type used is code id_token combined with TLS encryption",
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

public class FAPIRWID2ClientTestWithMTLSHolderOfKey extends AbstractFAPIRWID2ClientTest {

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
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
