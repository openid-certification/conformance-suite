package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.account.AccountListValidator;
import net.openid.conformance.openbanking_brasil.resourcesAPI.EnumResourcesType;
import net.openid.conformance.openbanking_brasil.resourcesAPI.ResourcesResponseValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "accounts-resources-api-test",
	displayName = "Validate structure of all accounts API resources",
	summary = "Validates the structure of all account API resources\n" +
		"\u2022 Cria Consentimento apenas com as Permissions necessárias para acessar os recursos da API de Accounts\n" +
		"\u2022 Valida todos os campos enviados na API de consentimento\n" +
		"\u2022 Chama todos os recursos da API de Accounts\n" +
		"\u2022 Valida todos os campos dos recursos da API de Accounts",
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
public class AccountsResourcesApiTestModule extends AccountApiTestModule{

	private static final String API_RESOURCE_ID = "accountId";
	private static final String RESOURCES_URL = "https://matls-api.mockbank.poc.raidiam.io/open-banking/resources/v1/resources";
	private static final String RESOURCE_TYPE = EnumResourcesType.ACCOUNT.name();


	@Override
	protected void validateResponse() {
		callAndContinueOnFailure(AccountListValidator.class, Condition.ConditionResult.FAILURE);
		env.putString("apiIdName", API_RESOURCE_ID);
		callAndStopOnFailure(ExtractAllSpecifiedApiIds.class);
		env.putString("protected_resource_url", RESOURCES_URL);
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
