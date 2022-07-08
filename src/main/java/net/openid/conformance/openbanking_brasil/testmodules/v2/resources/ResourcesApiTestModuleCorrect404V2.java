package net.openid.conformance.openbanking_brasil.testmodules.v2.resources;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.AddScopesForCustomerApi;
import net.openid.conformance.openbanking_brasil.testmodules.support.AbstractOBBrasilFunctionalTestModuleOptionalErrors;
import net.openid.conformance.openbanking_brasil.testmodules.support.BuildCreditOperationsAdvancesConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas404;
import net.openid.conformance.openbanking_brasil.testmodules.support.IgnoreResponseError;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareAllCustomerRelatedConsentsForResource404HappyPathTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "resources-api-test-404-customer-data-v2",
	displayName = "Validate correct response when only request customer data permissions",
	summary = "Validates correct response when only requesting customer data permissions\n" +
		"\u2022 Creates a Consent with the customer personal and business permissions (\"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\", \"CUSTOMERS_PERSONAL_ADITTIONALINFO_READ\", \"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ\", \"CUSTOMERS_BUSINESS_ADITTIONALINFO_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects a success 201 - Expects a success on Redirect as well \n" +
		"\u2022 Calls GET Resources API V2\n" +
		"\u2022 Expects a 404 response as neither customer personal nor customer business support the resources API V2",
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
public class ResourcesApiTestModuleCorrect404V2 extends AbstractOBBrasilFunctionalTestModuleOptionalErrors {

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildCreditOperationsAdvancesConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddScopesForCustomerApi.class);
		callAndStopOnFailure(IgnoreResponseError.class);
		callAndStopOnFailure(PrepareAllCustomerRelatedConsentsForResource404HappyPathTest.class);
	}

	@Override
	protected void validateResponse() {

		String logMessage = "Validate correct 404 api response";
		runInBlock(logMessage, () -> {
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseCodeWas404.class, Condition.ConditionResult.FAILURE);
		});
	}
}
