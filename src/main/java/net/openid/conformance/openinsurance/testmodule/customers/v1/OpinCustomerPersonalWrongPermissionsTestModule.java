package net.openid.conformance.openinsurance.testmodule.customers.v1;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractPermissionsCheckingFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddDummyPersonalProductTypeToConfig;
import net.openid.conformance.openbanking_brasil.testmodules.support.BuildPersonalCustomersConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.support.CallProtectedResourceExpectingFailureSequence;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas403;
import net.openid.conformance.openinsurance.testmodule.support.OpinConsentPermissionsBuilder;
import net.openid.conformance.openinsurance.testmodule.support.PermissionsGroup;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "opin-customer-personal-api-wrong-permissions-test",
	displayName = "Ensures API resource cannot be called with wrong permissions",
	summary = "Ensures API resource cannot be called with wrong permissions - there will be two browser interactions with this test\n" +
		"\u2022 Creates a Consent with the customer personal permissions ( \"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\",”CUSTOMERS_PERSONAL_QUALIFICATION_READ”\"CUSTOMERS_PERSONAL_ADITTIONALINFO_READ\",\"RESOURCES_READ”)\n" +
		"\u2022 Expects a success 201 - Checks all of the fields sent on the consent API are specification compliant\n" +
		"\u2022 Calls GET Personal Qualifications resources V2\n" +
		"\u2022 Expects a success 200\n" +
		"\u2022 Creates a Consent with all the permissions but the Customer Personal\n" +
		"\u2022 Expects a success 201 - Checks all of the fields sent on the consent API are specification compliantn" +
		"\u2022 Expects a 403\n" +
		"\u2022 Calls the GET Customer Personal Financial Relations Resource V2\n" +
		"\u2022 Expects a 403\n",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
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
public class OpinCustomerPersonalWrongPermissionsTestModule extends AbstractPermissionsCheckingFunctionalTestModule {
	OpinConsentPermissionsBuilder permissionsBuilder = new OpinConsentPermissionsBuilder(env,getId(),eventLog,testInfo,executionManager);

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildPersonalCustomersConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		//Simple UI fix
		callAndStopOnFailure(AddDummyPersonalProductTypeToConfig.class);
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
		callAndStopOnFailure(PrepareToGetPersonalQualifications.class);
	}

	@Override
	protected void prepareIncorrectPermissions() {
		permissionsBuilder.resetPermissions().addPermissionsGroup(PermissionsGroup.ALL)
			.removePermissionsGroups(PermissionsGroup.CUSTOMERS_PERSONAL)
			.removePermissionsGroups(PermissionsGroup.CUSTOMERS_BUSINESS);
	}

	@Override
	protected void requestResourcesWithIncorrectPermissions() {
		runInBlock("Ensure we cannot call the  Customer Personal Qualification", () -> {
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Customer Personal Identifications", () -> {
			callAndStopOnFailure(PrepareToGetPersonalIdentifications.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Customer Personal Complimentary Information", () -> {
			callAndStopOnFailure(PrepareToGetPersonalComplementaryInfo.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});
	}
}
