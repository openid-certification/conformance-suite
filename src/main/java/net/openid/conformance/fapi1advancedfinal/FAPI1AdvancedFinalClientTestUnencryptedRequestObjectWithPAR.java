package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.as.EnsureRequestObjectWasNotEncrypted;
import net.openid.conformance.condition.as.RemoveRequestObjectEncryptionValuesFromServerConfiguration;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi1-advanced-final-client-test-unencrypted-request-object-with-par",
	displayName = "FAPI1-Advanced-Final: test unencrypted request object support when using PAR",
	summary = "Tests a 'happy path' flow; using PAR and an unencrypted request object. " +
		"The client should perform OpenID discovery from the displayed discoveryUrl, " +
		"call the PAR endpoint with a request object which is NOT encrypted, " +
		"call the authorization endpoint (which will immediately redirect back), " +
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
	}
)
@VariantNotApplicable(parameter = FAPIAuthRequestMethod.class, values = "by_value")
public class FAPI1AdvancedFinalClientTestUnencryptedRequestObjectWithPAR extends AbstractFAPI1AdvancedFinalClientTest {

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

	@Override
	protected void onConfigurationCompleted() {
		super.onConfigurationCompleted();
		callAndStopOnFailure(RemoveRequestObjectEncryptionValuesFromServerConfiguration.class);
	}

	@Override
	protected void validateRequestObjectForPAREndpointRequest() {
		super.validateRequestObjectForPAREndpointRequest();
		callAndStopOnFailure(EnsureRequestObjectWasNotEncrypted.class);
	}
}
