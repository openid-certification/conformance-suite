package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.FinancingResponseValidator;
import net.openid.conformance.openbanking_brasil.resourcesAPI.EnumResourcesType;
import net.openid.conformance.openbanking_brasil.resourcesAPI.PrepareUrlForResourcesCall;
import net.openid.conformance.openbanking_brasil.resourcesAPI.ResourcesResponseValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "financings-resources-api-test",
	displayName = "Validate structure of financing API and Resources API resources",
	summary = "Makes sure that the Resource API and the API that is the scope of this test plan are returning the same available IDs\n" +
		"\u2022Create a consent with all the permissions needed to access the tested API\n" +
		"\u2022 Expects server to create the consent with 201\n" +
		"\u2022 Redirect the user to authorize at the financial institution\n" +
		"\u2022 Call the tested resource API\n" +
		"\u2022 Expect a success - Validate the fields of the response and Make sure that an id is returned - Fetch the id provided by this API\n" +
		"\u2022 Call the resources API\n" +
		"\u2022 Expect a success - Validate the fields of the response that are marked as AVAILABLE are exactly the ones that have been returned by the tested API",
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
		"resource.resourceUrl"
	}
)
public class FinancingsApiResourcesTestModule extends FinancingsApiTestModule {
	private static final String API_RESOURCE_ID = "contractId";
	private static final String RESOURCE_TYPE = EnumResourcesType.FINANCING.name();

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddResourcesScope.class);
		super.onConfigure(config, baseUrl);
	}

	@Override
	protected void validateResponse() {

		runInBlock("Validate financing root response", () -> {
			callAndStopOnFailure(FinancingResponseValidator.class);
			callAndStopOnFailure(EnsureResponseHasLinks.class);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
			call(sequence(ValidateSelfEndpoint.class));
		});

		env.putString("apiIdName", API_RESOURCE_ID);
		callAndStopOnFailure(ExtractAllSpecifiedApiIds.class);
		callAndStopOnFailure(PrepareUrlForResourcesCall.class);
		preCallProtectedResource("Call Resources API");

		runInBlock("Validate Resources response", () -> {
			callAndStopOnFailure(ResourcesResponseValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseHasLinks.class);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
			call(sequence(ValidateSelfEndpoint.class));
		});

		eventLog.startBlock("Compare active resourceId's with API resources");
		env.putString("resource_type", RESOURCE_TYPE);
		callAndStopOnFailure(ExtractResourceIdOfActiveResources.class);
		callAndStopOnFailure(CompareResourceIdWithAPIResourceId.class);
		eventLog.endBlock();
	}
}

