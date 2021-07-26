package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditOperations.advances.*;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "unarranged-overdraft-api-test",
	displayName = "Validate structure of all unarranged overdraft API resources",
	summary = "Validates the structure of all unarranged overdraft API resources",
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
public class CreditOperationsAdvancesApiTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareAllCreditOperationsPermissionsForHappyPath.class);
	}

	@Override
	protected void validateResponse() {
		callAndContinueOnFailure(AdvancesResponseValidator.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(CreditAdvanceSelector.class);

		callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContracts.class);
		preCallProtectedResource("Contracts");
		callAndContinueOnFailure(AdvancesContractResponseValidator.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractGuarantees.class);
		preCallProtectedResource("Contract Guarantees");
		callAndContinueOnFailure(AdvancesGuaranteesResponseValidator.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractPayments.class);
		preCallProtectedResource("Contract Payments");
		callAndContinueOnFailure(AdvancesPaymentsResponseValidator.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractInstallments.class);
		preCallProtectedResource("Contract Installments");
		callAndContinueOnFailure(AdvancesContractInstallmentsResponseValidator.class, Condition.ConditionResult.FAILURE);
	}
}
