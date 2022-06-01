package net.openid.conformance.openbanking_brasil.testmodules.customerAPI;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.FAPIBrazilAddExpirationToConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilCreateConsentRequest;
import net.openid.conformance.condition.client.SetConsentsScopeOnTokenEndpointRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "customer-api-businessentity-personal-permissions",
	displayName = "Validate the structure of all consent API resources",
	summary = "This test makes sure that if businessEntity is sent on the consent creation together with personal customer data permission the server will reject the request\n" +
		"\u2022 Make sure that the user has sent a data Payload that contains a BusinessEntity \n" +
		"\u2022 Make a consent request with all the permissions but the Customer Business ones \n",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"consent.productType"
	}
)
public class BusinessEntityWithPersonalPermissionsTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected void runTests() {

		runInBlock("Validating create consent response", () -> {
			callAndStopOnFailure(EnsureBrazilCNPJ.class);
			callAndStopOnFailure(ForcePersonalProductType.class);
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(SetConsentsScopeOnTokenEndpointRequest.class);
			callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
			callAndStopOnFailure(SetContentTypeApplicationJson.class);
			callAndStopOnFailure(EnsureBusinessEntityInConsentRequest.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.INFO);
			callAndStopOnFailure(EnsureResponseCodeWas400.class);
			callAndStopOnFailure(ValidateErrorFromResourceEndpointResponseError.class);
		});
	}


}