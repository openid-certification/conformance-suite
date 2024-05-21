package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.as.EncryptIdToken;
import net.openid.conformance.condition.as.EnsureIdTokenEncryptedResponseAlgIsNotRSA1_5;
import net.openid.conformance.condition.as.FAPIEnsureClientJwksContainsAnEncryptionKey;
import net.openid.conformance.condition.as.dynregistration.EnsureIdTokenEncryptedResponseAlgIsSetIfEncIsSet;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi1-advanced-final-client-test-encrypted-idtoken",
	displayName = "FAPI1-Advanced-Final: client test encrypted id_token",
	summary = "Tests a 'happy path' flow with encrypted id_tokens. This test uses client2. " +
		"The client should perform OpenID discovery from the displayed discoveryUrl, " +
		"call the authorization endpoint (which will immediately redirect back with an encrypted id_token), " +
		"exchange the authorization code for an access token at the token endpoint " +
		"and make a request to the accounts/payments/resources endpoint displayed..",
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
// The new Brazil security profile requires encryption to always be used, so all tests encrypt and hence this test is not necessary
@VariantNotApplicable(parameter = FAPI1FinalOPProfile.class, values = { "openbanking_brazil", "openinsurance_brazil" })
public class FAPI1AdvancedFinalClientTestEncryptedIdToken extends AbstractFAPI1AdvancedFinalClientTest {

	@Override
	protected void configureClients() {
		super.configureClients();
		configureSecondClient();
	}

	@Override
	protected void onConfigurationCompleted() {
		//run the whole test with the second client
		switchToSecondClient();
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
			callAndStopOnFailure(EnsureIdTokenEncryptedResponseAlgIsNotRSA1_5.class, "FAPI1-ADV-8.6.1-1");
			callAndStopOnFailure(FAPIEnsureClientJwksContainsAnEncryptionKey.class, "FAPI1-ADV-5.2.3.1-5", "FAPI1-ADV-8.6.1-1");
			callAndStopOnFailure(EnsureIdTokenEncryptedResponseAlgIsSetIfEncIsSet.class, "OIDCR-2", "FAPI1-ADV-5.2.2.1-6");
		}
	}

	@Override
	protected void encryptIdToken(boolean isAuthorizationEndpoint) {
		callAndStopOnFailure(EncryptIdToken.class, "OIDCC-10.2", "FAPI1-ADV-5.2.2.1-6");
	}
}
