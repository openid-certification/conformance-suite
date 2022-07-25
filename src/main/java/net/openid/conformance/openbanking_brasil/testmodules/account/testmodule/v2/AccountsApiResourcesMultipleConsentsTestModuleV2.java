package net.openid.conformance.openbanking_brasil.testmodules.account.testmodule.v2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.resourcesAPI.EnumResourcesStatus;
import net.openid.conformance.openbanking_brasil.resourcesAPI.EnumResourcesType;
import net.openid.conformance.openbanking_brasil.resourcesAPI.v2.PrepareUrlForResourcesCallV2;
import net.openid.conformance.openbanking_brasil.resourcesAPI.v2.ResourcesResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.account.BuildAccountsConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.account.PrepareUrlForFetchingAccountBalances;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages.TestTimedOut;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "accounts-resources-test-multiple-consents-v2",
	displayName = "Validates that the server has correctly implemented the rules set for joint accounts that require multiple consents for data to be shared.",
	summary =
		"\u2022 Create a CONSENT with only ACCOUNTS_READ, ACCOUNTS_BALANCES_READ and RESOURCES_READ Permissions using the CPF and CNPJ provided for joint accounts\n" +
			"\u2022 Expect a Success 201\n" +
			"\u2022 Redirect the user to authorize the Consent - Redirect URI must contain accounts, resources and consents scopes\n" +
			"\u2022 Expect a Successful authorization with an authorization code created\n" +
			"\u2022 Call the RESOURCES API with the authorized consent\n" +
			"\u2022 Expect a 200 - Validate that one Account Resource has been returned and it is on the state AWAITING_AUTHORIZATION\n" +
			"\u2022 Call the ACCOUNTS API\n" +
			"\u2022 Expect a 200 - Make sure the Server returns a 200 with an empty list on the object\n" +
			"\u2022 Call the ACCOUNTS BALANCES API with the Account ID of the Account on AWAITING_AUTHORIZATION\n" +
			"\u2022 Expect a 403 - Validate that the field response.errors.code is STATUS_RESOURCE_AWAITING_AUTHORIZATION\n" +
			"\u2022 POLL the GET RESOURCES API for 5 minutes, one call every 30 seconds.\n" +
			"\u2022 Continue Polling until the Account Resource returned is on the status AVAILABLE\n" +
			"\u2022 Call the ACCOUNTS API\n" +
			"\u2022 Expect a 200 - Make sure the Account Resource is now returned on the API response\n",
	profile = OBBProfile.OBB_PROFIlE_PHASE2_VERSION2,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpfJointAccount",
		"resource.brazilCnpjJointAccount",
		"consent.productType"
	}
)
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"directory.client_id",
	"client.org_jwks"
})
public class AccountsApiResourcesMultipleConsentsTestModuleV2 extends AbstractOBBrasilFunctionalTestModule {

	private static final String RESOURCE_TYPE = EnumResourcesType.ACCOUNT.name();

	private static final String API_RESOURCE_ID = "accountId";

	@Override
	protected void configureClient() {
		super.configureClient();
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		env.putBoolean("continue_test", true);
		callAndContinueOnFailure(EnsureJointAccountCpfOrCnpjIsPresent.class, Condition.ConditionResult.WARNING);
		if (!env.getBoolean("continue_test")) {
			fireTestFinished();
		}
	}

	@Override
	protected void requestProtectedResource() {
		// Call Resources API
		callAndStopOnFailure(PrepareUrlForResourcesCallV2.class);
		runInBlock("Call Resources V2 API", () -> call(getPreCallProtectedResourceSequence()));

		runInBlock("Validate Resources V2 response", () -> {
			callAndStopOnFailure(ResourcesResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseHasLinks.class);

			env.putString("resource_type", RESOURCE_TYPE);
			env.putString("resource_status", EnumResourcesStatus.PENDING_AUTHORISATION.name());
			callAndStopOnFailure(ExtractResourceIdOfResourcesWithSpecifiedTypeAndStatus.class);

			env.putString("environment_key", API_RESOURCE_ID);
			callAndStopOnFailure(ExtractFirstResourceIdToSpecifiedEnvironmentKey.class);

		});

		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		super.requestProtectedResource(); // Call Accounts API
	}

	@Override
	protected void validateResponse() {
		// Accounts Validation
		callAndStopOnFailure(EnsureAccountListIsEmpty.class);
		callAndStopOnFailure(EnsureResponseHasLinks.class);

		// Call ACCOUNTS BALANCES API
		callAndStopOnFailure(PrepareUrlForFetchingAccountBalances.class);
		runInBlock("Call Accounts Balances API", () -> call(getPreCallProtectedResourceSequence()
			.replace(EnsureResponseCodeWas200.class, condition(EnsureResponseCodeWas403.class))
		));

		runInBlock("Validate Accounts Balances response", () -> {
			callAndStopOnFailure(ResourceErrorMetaValidator.class);
			callAndStopOnFailure(EnsureErrorResponseCodeIsStatusResourceAwaitingAuthorization.class);

		});

		// Poll Resources API
		runInBlock("Poll Resources API", () -> {
			callAndStopOnFailure(PrepareUrlForResourcesCallV2.class);

			env.putString("resource_status", EnumResourcesStatus.AVAILABLE.name());
			env.putString("resource_id", env.getString("accountId"));
			repeatSequence(() -> getPreCallProtectedResourceSequence()
				.then(getPollingSequence()))
				.untilTrue("resource_found")
				.times(10)
				.trailingPause(30)
				.onTimeout(sequenceOf(
					condition(TestTimedOut.class),
					condition(ChuckWarning.class)))
				.run();

		});

		// Call accounts API
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		runInBlock("Call Accounts API", () -> call(getPreCallProtectedResourceSequence()));

		runInBlock("Validate Accounts response", () -> {
			env.putString("apiIdName", API_RESOURCE_ID);
			callAndStopOnFailure(EnsureResponseHasLinks.class);
			callAndStopOnFailure(ExtractAllSpecifiedApiIds.class);
			callAndStopOnFailure(EnsureSpecifiedIdIsPresent.class);
		});


	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddAccountScope.class);
		callAndStopOnFailure(AddResourcesScope.class);
		callAndStopOnFailure(PrepareResourceAccountBalancesReadOnlyConsentPermissions.class);
	}

	protected ConditionSequence getPollingSequence() {
		return sequenceOf(
			condition(ResourcesResponseValidatorV2.class),
			condition(EnsureResponseHasLinks.class),
			condition(ExtractResourceIdOfResourcesWithSpecifiedTypeAndStatus.class),
			condition(FindSpecifiedResourceId.class)
		);
	}

	protected ConditionSequence getPreCallProtectedResourceSequence() {
		return sequenceOf(
			condition(CreateEmptyResourceEndpointRequestHeaders.class),
			condition(AddFAPIAuthDateToResourceEndpointRequest.class),
			condition(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class),
			condition(CreateRandomFAPIInteractionId.class),
			condition(AddFAPIInteractionIdToResourceEndpointRequest.class),
			condition(CallProtectedResource.class),
			condition(EnsureResponseCodeWas200.class),
			condition(CheckForDateHeaderInResourceResponse.class),
			condition(CheckForFAPIInteractionIdInResourceResponse.class),
			condition(EnsureResourceResponseReturnedJsonContentType.class)
		);
	}
}
