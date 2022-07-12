package net.openid.conformance.openbanking_brasil.testmodules.customerAPI.testmodule.v2;

import com.google.gson.JsonObject;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.registrationData.v2.BusinessIdentificationValidatorV2;
import net.openid.conformance.openbanking_brasil.registrationData.v2.BusinessQualificationResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.registrationData.v2.PersonalIdentificationResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.registrationData.v2.PersonalQualificationResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;


@PublishTestModule(
	testName = "customer-personal-api-operational-limits",
	displayName = "Make sure that the server is not blocking access to the APIs as long as the operational limits for the Customer Personal API are considered correctly",
	summary = "The test will require a DCR to be executed prior to the test against a server whose credentials are provided here https://gitlab.com/obb1/certification/-/wikis/Operational-Limits\n" +
		"This test will require the user to have set at least two ACTIVE resources on the Customer Personal API." +
		"This test will make sure that the server is not blocking access to the APIs as long as the operational limits for the Customer Personal API are considered correctly.\n" +
		"\u2022 Make Sure that the fields “Client_id for Operational Limits Test” (client_id for OL) and at least the CPF for Operational Limits (CPF for OL) test have been provided\n" +
		"\u2022 Using the HardCoded clients provided on the test summary link, use the client_id for OL and the CPF/CNPJ for OL passed on the configuration and create a Consent Request sending the Customer Personal permission group - Expect Server to return a 201 - Save ConsentID (1)\n" +
		"\u2022 Return a Success if Consent Response is a 201 containing all permissions required on the scope of the test. Return a Warning and end the test if the consent request returns either a 422 or a 201 without Permission for this specific test.\n" +
		"\u2022 With the authorized consent id (1), call the GET Customer Personal Identifications API - Expect a 200 response\n" +
		"\u2022 With the authorized consent id (1), call the GET Customer Personal Qualifications- Expect a 200 response\n" +
		"\u2022 With the authorized consent id (1), call the GET Customer Personal Financial Relations - Expect a 200 response\n" +
		"\u2022 Using the regular client_id provided and the regular CPF/CNPJ for OL create a Consent Request sending the Customer Personal permission group - Expect Server to return a 201 - Save ConsentID (2)\n" +
		"\u2022 Redirect User to authorize the Created Consent - Expect a successful authorization\n" +
		"\u2022 With the authorized consent id (2), call the GET Customer Personal Identifications API - Expect a 200 response\n" +
		"\u2022 With the authorized consent id (2), call the GET Customer Personal Qualifications- Expect a 200 response\n" +
		"\u2022 With the authorized consent id (2), call the GET Customer Personal Financial Relations - Expect a 200 response",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.client_id_operational_limits",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.brazilCnpj",
		"resource.brazilCpfOperational",
		"resource.brazilCnpjOperational"
	}
)
public class CustomerPersonalApiOperationalLimitsTestModuleV2 extends AbstractOBBrasilFunctionalTestModule {

	private int numberOfExecutions = 1;

	@Override
	protected void configureClient() {
		callAndContinueOnFailure(BuildPersonalCustomersConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(EnsureClientIdForOperationalLimitsIsPresent.class);
		callAndStopOnFailure(AddScopesForCustomerApi.class);
		callAndStopOnFailure(PrepareAllCustomerPersonalRelatedConsentsForHappyPathTest.class);
		callAndStopOnFailure(AddDummyPersonalProductTypeToConfig.class);
		callAndStopOnFailure(SwitchToOperationalLimitsClientId.class);
		callAndContinueOnFailure(OperationalLimitsToConsentRequest.class);
	}


	@Override
	protected void validateResponse() {
		callAndStopOnFailure(EnsureResponseCodeWas200.class);
		callAndStopOnFailure(PersonalIdentificationResponseValidatorV2.class);
		callAndStopOnFailure(ValidateResponseMetaData.class);
		eventLog.endBlock();

		callAndStopOnFailure(PrepareToGetPersonalQualifications.class);
		preCallProtectedResource("Calling Business Qualifications Endpoint with consent_id_" + numberOfExecutions);
		runInBlock("Validate Business Qualifications response", () -> {
			callAndStopOnFailure(EnsureResponseCodeWas200.class);
			callAndStopOnFailure(PersonalQualificationResponseValidatorV2.class);
			callAndStopOnFailure(ValidateResponseMetaData.class);
		});

		callAndStopOnFailure(PrepareToGetPersonalFinancialRelationships.class);
		preCallProtectedResource("Calling Customer Business Financial Relations Endpoint with consent_id_" + numberOfExecutions);
		runInBlock("Validate Customer Business Financial Relations response", () -> {
			callAndStopOnFailure(EnsureResponseCodeWas200.class);
			callAndStopOnFailure(ValidateResponseMetaData.class);
		});
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		if(numberOfExecutions == 1){
			callAndStopOnFailure(PrepareToGetPersonalIdentifications.class);
			callAndStopOnFailure(SwitchToOriginalClientId.class);
			callAndStopOnFailure(RemoveOperationalLimitsFromConsentRequest.class);
			callAndStopOnFailure(RemoveConsentIdFromClientScopes.class);
			validationStarted = false;
			numberOfExecutions++;
			performAuthorizationFlow();
		}else {
			fireTestFinished();
		}
	}
}
