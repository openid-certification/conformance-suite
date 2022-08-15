package net.openid.conformance.openbanking_brasil.testmodules.v2.resources;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.AddScopesForCustomerApi;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "resources-api-test-404-customer-data-v2",
	displayName = "Validate correct response when only request customer data permissions",
	summary = "Validates correct response when only requesting customer data permissions\n" +
		"\u2022 Creates a Consent with either the customer personal or business permissions (\"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\", \"CUSTOMERS_PERSONAL_ADITTIONALINFO_READ\", \"RESOURCES_READ\") or (\"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ\", \"CUSTOMERS_BUSINESS_ADITTIONALINFO_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects a success 201 - Expects a success on Redirect as well \n" +
		"\u2022 Calls GET Resources API V2\n" +
		"\u2022 Expects a 200 response with empty data object",
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
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks"
})
public class ResourcesApiTestModuleCorrect404V2 extends AbstractOBBrasilFunctionalTestModuleOptionalErrors {

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildResourcesConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddResourcesScope.class);
		callAndStopOnFailure(IgnoreResponseError.class);
		callAndStopOnFailure(PrepareAllCustomerRelatedConsentsForResource404HappyPathTest.class);
	}

	@Override
	protected void validateResponse() {

		String logMessage = "Validate correct 200 api response with empty data object";
		runInBlock(logMessage, () -> {
			callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureDataArrayIsEmpty.class);
		});
	}
}
