package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalBrazilDCR;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.sequence.client.CallDynamicRegistrationEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "dcr-subjectdn",
	displayName = "Resources API test that DCR works with both numeric and string oids",
	summary = "\u2022 Obtains a software statement from the Brazil sandbox directory.\n" +
		"\u2022 Registers a new client on the target authorization server." +
		" This is done twice - one where the Brazil specific OIDs are in numeric form (which must be accepted), and one with them in the string form (which should be accepted).\n",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl"
	}
)
@VariantNotApplicable(parameter = ClientAuthType.class, values = {"private_key_jwt"})// only applicable for mtls client auth
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"resource.brazilOrganizationId",
	"resource.brazilPaymentConsent",
	"resource.brazilPixPayment",
})
public class DcrSubjectDnTestModule extends AbstractFAPI1AdvancedFinalBrazilDCR {

	boolean useBrazilShortNames = true;


	@Override
	protected void configureClient() {
		super.configureClient();
		deleteClient();

		// again but with non-RFC OIDs in numeric form
		useBrazilShortNames = false;
		super.configureClient();
		eventLog.startBlock("Delete client");
		deleteClient();
	}

	@Override
	protected void callRegistrationEndpoint() {
		if (useBrazilShortNames) {
			callAndStopOnFailure(CallDynamicRegistrationEndpoint.class, "RFC7591-3.1", "OIDCR-3.2");

			call(exec().mapKey("endpoint_response", "dynamic_registration_endpoint_response"));

			callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");
			callAndStopOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");

			// this is all lifted out of 'super'
			callAndContinueOnFailure(CheckNoErrorFromDynamicRegistrationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");
			callAndStopOnFailure(ExtractDynamicRegistrationResponse.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");
			callAndContinueOnFailure(VerifyClientManagementCredentials.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");
			callAndContinueOnFailure(ClientManagementEndpointAndAccessTokenRequired.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2");

			call(exec().unmapKey("endpoint_response"));
		} else {
			call(sequence(CallDynamicRegistrationEndpointAndVerifySuccessfulResponse.class));
			callAndContinueOnFailure(ClientManagementEndpointAndAccessTokenRequired.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2");
			eventLog.endBlock();
		}
	}

	@Override
	protected void addTlsClientAuthSubjectDn() {
		if (useBrazilShortNames) {
			callAndStopOnFailure(AddTlsClientAuthSubjectDnWithBrazilShortnameToDynamicRegistrationRequest.class);
		} else {
			super.addTlsClientAuthSubjectDn();
		}
	}

	@Override
	public void start() {
		fireTestFinished();
	}

	@Override
	protected void setupResourceEndpoint() {
		// not needed as resource endpoint won't be called
	}

	@Override
	protected boolean scopeContains(String requiredScope) {
		// Not needed as scope field is optional
		return false;
	}

}
