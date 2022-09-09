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
	displayName = "Resources API test that DCR works",
	summary = "\u2022 Obtains a software statement from the Brazil sandbox directory.\n" +
		"\u2022 Registers a new client on the target authorization server.",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl"
	}
)
@VariantNotApplicable(parameter = ClientAuthType.class, values = {"private_key_jwt"})// only applicable for mtls client auth
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks",
	"resource.brazilPaymentConsent",
	"resource.brazilPixPayment",
	"resource.brazilCpf",
	"resource.brazilCnpj"
})
public class DcrSubjectDnTestModule extends AbstractFAPI1AdvancedFinalBrazilDCR {

	@Override
	protected void configureClient() {
		super.configureClient();
		eventLog.startBlock("Delete client");
		deleteClient();
	}

	@Override
	protected void callRegistrationEndpoint() {
		call(sequence(CallDynamicRegistrationEndpointAndVerifySuccessfulResponse.class));
		callAndContinueOnFailure(ClientManagementEndpointAndAccessTokenRequired.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2");
		eventLog.endBlock();
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
