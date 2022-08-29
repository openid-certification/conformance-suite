package net.openid.conformance.openinsurance.testmodule.customers.v1;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractPermissionsCheckingFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openinsurance.testmodule.support.OpinConsentPermissionsBuilder;
import net.openid.conformance.openinsurance.testmodule.support.PermissionsGroup;
import net.openid.conformance.testmodule.PublishTestModule;


@PublishTestModule(
	testName = "opin-customer-business-api-wrong-permissions-test-v2",
	displayName = "Ensures API resource cannot be called with wrong permissions V2",
	summary = "Ensures API resource cannot be called with wrong permissions - there will be two browser interactions with this test\n" +
		"\u2022 Creates a Consent will the customer business permissions ((\"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ\",”CUSTOMERS_BUSINESS_QUALIFICATION_READ”, \"CUSTOMERS_BUSINESS_ADITTIONALINFO_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects a success 201 - Check all of the fields sent on the consent API is spec compliant \n" +
		"\u2022 Calls GET Business Qualifications resource\n" +
		"\u2022 Expects a success 200\n" +
		"\u2022 Creates a Consent with all the permissions but the Customer ones \n" +
		"\u2022 Calls the GET Customer Business Qualification Resource\n" +
		"\u2022 Expects a 403\n" +
		"\u2022 Calls the GET Customer Business Complimentary-Information Endpoint\n" +
		"\u2022 Expects a 403",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf"
	}
)
public class OpinCustomerBusinessWrongPermissionsTestModule extends AbstractPermissionsCheckingFunctionalTestModule {

	OpinConsentPermissionsBuilder permissionsBuilder = new OpinConsentPermissionsBuilder(env,getId(),eventLog,testInfo,executionManager);

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildBusinessCustomersConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		//Simple UI fix
		callAndStopOnFailure(AddDummyBusinessProductTypeToConfig.class);
		callAndStopOnFailure(AddScopesForCustomerApi.class);
		super.onConfigure(config, baseUrl);
	}

	@Override
	protected void preFetchResources() {
		//Not needed for this test
	}

	@Override
	protected void prepareCorrectConsents() {
		permissionsBuilder.resetPermissions().addPermissionsGroup(PermissionsGroup.CUSTOMERS_PERSONAL);
		callAndStopOnFailure(PrepareToGetBusinessQualifications.class);
	}

	@Override
	protected void prepareIncorrectPermissions() {
		permissionsBuilder.resetPermissions().addPermissionsGroup(PermissionsGroup.ALL)
			.removePermissionsGroups(PermissionsGroup.CUSTOMERS_PERSONAL)
			.removePermissionsGroups(PermissionsGroup.CUSTOMERS_BUSINESS);
	}

	@Override
	protected void requestResourcesWithIncorrectPermissions() {
		runInBlock("Ensure we cannot call the  Customer Business Qualification", () -> {
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Customer Business Identifications", () -> {
			callAndStopOnFailure(PrepareToGetBusinessIdentifications.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Customer Business Complimentary-Information", () -> {
			callAndStopOnFailure(PrepareToGetBusinessComplimentaryInfo.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});
	}
}
