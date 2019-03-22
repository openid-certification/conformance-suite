package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.as.RemoveAtHashFromIdToken;
import io.fintechlabs.testframework.condition.as.AddPrivateKeyJWTToServerConfiguration;
import io.fintechlabs.testframework.condition.as.EnsureClientAssertionTypeIsJwt;
import io.fintechlabs.testframework.condition.as.ExtractClientAssertion;
import io.fintechlabs.testframework.condition.as.ValidateClientAssertionClaims;
import io.fintechlabs.testframework.condition.as.ValidateClientSigningKeySize;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ob-client-test-code-id-token-with-private-key-jwt-and-matls-missing-athash",
	displayName = "FAPI-OB: client test - id_token without an at_hash value from the authorization_endpoint should be rejected(code id_token with private_key_jwt and MATLS)",
	profile = "FAPI-OB",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
	}
)

public class FAPIOBClientTestCodeIdTokenWithPrivateKeyJWTAndMATLSNoAtHash extends AbstractFAPIOBClientTestCodeIdToken {

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

		callAndStopOnFailure(RemoveAtHashFromIdToken.class, "OIDCC-3.2.2.9");
	}

}
