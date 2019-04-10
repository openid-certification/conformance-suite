package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.as.AddTLSClientAuthToServerConfiguration;
import io.fintechlabs.testframework.condition.as.EnsureNoClientAssertionSentToTokenEndpoint;
import io.fintechlabs.testframework.condition.as.RemoveAtHashFromIdToken;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-missing-athash",
	displayName = "FAPI-RW-ID2-OB: client test - id_token without an at_hash value from the authorization_endpoint should be rejected(with mtls holder of key)",
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

public class FAPIOBClientTestCodeIdTokenWithMATLSNoAtHash extends AbstractFAPIOBClientTestCodeIdToken {

	@Override
	protected void addTokenEndpointAuthMethodSupported() {

		callAndContinueOnFailure(AddTLSClientAuthToServerConfiguration.class);
	}

	@Override
	protected void validateClientAuthentication() {

		//Parent class has already verified the presented TLS certificate so nothing to do here.

		callAndStopOnFailure(EnsureNoClientAssertionSentToTokenEndpoint.class);

	}

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(RemoveAtHashFromIdToken.class, "OIDCC-3.3.2.9");
	}

}
