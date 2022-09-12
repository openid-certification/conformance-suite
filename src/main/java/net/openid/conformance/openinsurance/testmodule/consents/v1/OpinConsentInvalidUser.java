package net.openid.conformance.openinsurance.testmodule.consents.v1;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.account.v2.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.account.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages.ConsentHasExpiredInsteadOfBeenRejected;
import net.openid.conformance.openinsurance.testmodule.support.OpinConsentPermissionsBuilder;
import net.openid.conformance.openinsurance.testmodule.support.PermissionsGroup;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@PublishTestModule(
	testName = "opin-consent-inavlid-user-test",
	displayName = "This test will use a dummy, but well-formated payload to make sure that the server will accept the POST Consents request, as mandated by the security guidelines, however, will not be able to complete the authorization code flow as no user with this CPF exists on the financial institution",
	summary = "This test will use a dummy, but well-formated payload to make sure that the server will accept the POST Consents request, as mandated by the security guidelines, however, will not be able to complete the authorization code flow as no user with this CPF exists on the financial institution\n" +
		"\u2022 Call the POST Consents API with the dummy payload\n" +
		"\u2022 Expect the server to accept the message and return the 201 as the financial institution should not validate the CPF at that stage of the process.\n" +
		"\u2022 Redirect the user to authorize the consent\n" +
		"\u2022 Expect a failure an error on the authorization redirect",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
	}
)
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks"
})
public class OpinConsentInvalidUser extends AbstractOBBrasilFunctionalTestModule {

	OpinConsentPermissionsBuilder permissionsBuilder;
	@Override
	protected void configureClient(){
		//Arbitrary resource
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddConsentScope.class);
		callAndStopOnFailure(AddDummyCPFToConfig.class);
		callAndStopOnFailure(AddDummyPersonalProductTypeToConfig.class);

		permissionsBuilder = new OpinConsentPermissionsBuilder(env,getId(),eventLog,testInfo,executionManager);
		permissionsBuilder.resetPermissions()
			.addPermissionsGroup(PermissionsGroup.ALL)
			.removePermissionsGroups(PermissionsGroup.CUSTOMERS_BUSINESS)
			.build();
	}

	@Override
	protected void onAuthorizationCallbackResponse() {

		callAndContinueOnFailure(CheckMatchingCallbackParameters.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(RejectStateInUrlQueryForHybridFlow.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		callAndStopOnFailure(CheckAuthorizationEndpointHasError.class);

		callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.2.2.5", "JARM-4.4-2");

		callAndContinueOnFailure(ValidateIssInAuthorizationResponse.class, Condition.ConditionResult.WARNING, "OAuth2-iss-2");

		fireTestFinished();
	}


	@Override
	protected void validateResponse() {

	}
}
