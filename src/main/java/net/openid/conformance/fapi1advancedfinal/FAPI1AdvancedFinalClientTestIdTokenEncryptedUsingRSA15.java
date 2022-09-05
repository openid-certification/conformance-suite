package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.as.ChangeIdTokenEncryptedResponseAlgToRSA15;
import net.openid.conformance.condition.as.EncryptIdToken;
import net.openid.conformance.condition.as.FAPIEnsureClientJwksContainsAnEncryptionKey;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi1-advanced-final-client-test-encrypted-idtoken-usingrsa15",
	displayName = "FAPI1-Advanced-Final: client test id_token encrypted using RSA1_5 algorithm",
	summary = "This test uses client2. " +
		"The client should perform OpenID discovery from the displayed discoveryUrl, " +
		"call the authorization endpoint which will immediately redirect back with an id_token " +
		"encrypted using RSA1_5 algorithm which is not allowed.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"client2.client_id",
		"client2.scope",
		"client2.redirect_uri",
		"client2.certificate",
		"client2.jwks",
		"client2.id_token_encrypted_response_alg",
		"client2.id_token_encrypted_response_enc"
	}
)
@VariantNotApplicable(parameter = FAPIResponseMode.class, values = {"jarm"})
public class FAPI1AdvancedFinalClientTestIdTokenEncryptedUsingRSA15 extends AbstractFAPI1AdvancedFinalClientTest {

	@Override
	protected void configureClients() {
		super.configureClients();
		configureSecondClient();
	}

	@Override
	protected void onConfigurationCompleted() {
		//run the whole test with the second client
		switchToSecondClient();

		String alg = env.getString("client", "id_token_encrypted_response_alg");
		if(!alg.startsWith("RSA")) {
			fireTestSkipped("This test is only applicable when using an RSA encryption key, so the test has been skipped");
		}
	}

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

	@Override
	protected void validateClientJwks(boolean isSecondClient) {
		super.validateClientJwks(isSecondClient);
		if(isSecondClient) {
			//ensure that there is a key we can use for id_token encryption
			callAndStopOnFailure(FAPIEnsureClientJwksContainsAnEncryptionKey.class, "FAPI1-ADV-5.2.3.1-5", "FAPI1-ADV-8.6.1-1");
		}
	}

	@Override
	protected void encryptIdToken(boolean isAuthorizationEndpoint) {
		callAndStopOnFailure(ChangeIdTokenEncryptedResponseAlgToRSA15.class, "FAPI1-ADV-8.6.1-1");
		callAndStopOnFailure(EncryptIdToken.class, "OIDCC-10.2", "FAPI1-ADV-5.2.2.1-6");
		startWaitingForTimeout();
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {
		throw new TestFailureException(getId(), "Client called the token endpoint after receiving an id_token encrypted " +
			"using RSA1_5 algorithm. The client should have stopped after receiving an invalid id_token.");
	}
}
