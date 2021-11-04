package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.client.AddTlsClientAuthSubjectDnToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddTlsClientAuthSubjectDnWithBrazilShortnameToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.GetResourceEndpointConfiguration;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalBrazilDCR;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsurePaymentDateIsToday;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideClientWithPagtoClient;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetDirectoryInfo;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "payments-api-dcr-subjectdn",
	displayName = "Payments API test that DCR works with both numeric and string oids",
	summary = "Obtain a software statement from the Brazil sandbox directory (using a hardcoded client that has the PAGTO role), register a new client on the target authorization server and try the client credentials grant. This is done twice - one where the Brazil specific OIDs are in numeric form, and one with them in the string form - servers must accept both. Note that this test overrides the 'alias' value in the configuration, so you may see your test being interrupted if other users are testing.",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"resource.resourceUrl"
	}
)
@VariantNotApplicable(parameter = ClientAuthType.class, values = { "private_key_jwt" }) // only applicable for mtls client auth
public class PaymentsApiDcrSubjectDn extends AbstractFAPI1AdvancedFinalBrazilDCR {
	boolean useBrazilShortNames = false;

	@Override
	protected void configureClient() {
		brazilPayments = true;
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		callAndStopOnFailure(OverrideClientWithPagtoClient.class);
		callAndStopOnFailure(SetDirectoryInfo.class);
		callAndStopOnFailure(GetResourceEndpointConfiguration.class);
		super.configureClient();

		super.performPreAuthorizationSteps();

		deleteClient();

		// again but with string form
		useBrazilShortNames = true;
		super.configureClient();

		super.performPreAuthorizationSteps();

		deleteClient();

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
}
