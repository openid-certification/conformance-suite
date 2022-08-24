package net.openid.conformance.openinsurance.testmodule.deprecated.customers;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallConsentEndpointWithBearerToken;
import net.openid.conformance.condition.client.FAPIBrazilAddExpirationToConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilCreateConsentRequest;
import net.openid.conformance.condition.client.SetConsentsScopeOnTokenEndpointRequest;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureBrazilCNPJ;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureBusinessEntityInConsentRequest;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureConsentResponseWas400;
import net.openid.conformance.openbanking_brasil.testmodules.support.ForcePersonalProductType;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToPostConsentRequest;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetContentTypeApplicationJson;
import net.openid.conformance.openbanking_brasil.testmodules.support.ValidateErrorFromResourceEndpointResponseError;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "opin-customers-api-business-entity-personal-permissions-v1",
	displayName = "Validate the structure of all consent API resources",
	summary = "This test makes sure that if businessEntity is sent on the consent creation together with personal customer data permission the server will reject the request\n" +
		"\u2022 Make sure that the user has sent a data Payload that contains a BusinessEntity \n" +
		"\u2022 Make a consent request with all the permissions but the Customer Business ones \n",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE1,
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
public class OpinCustomersBusinessEntityWithPersonalPermissionsTestModuleV1 extends AbstractClientCredentialsGrantFunctionalTestModule {

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
			callAndContinueOnFailure(CallConsentEndpointWithBearerToken.class, Condition.ConditionResult.INFO);
			callAndStopOnFailure(EnsureConsentResponseWas400.class);
			call(exec().mapKey("resource_endpoint_response", "consent_endpoint_response"));
			callAndStopOnFailure(ValidateErrorFromResourceEndpointResponseError.class);
		});
	}


}
