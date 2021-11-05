package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddIatToRequestObject;
import net.openid.conformance.condition.client.AddJtiAsUuidToRequestObject;
import net.openid.conformance.condition.client.AddTlsClientAuthSubjectDnWithBrazilShortnameToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClient;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400or401;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400or401;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.FAPIBrazilCallPaymentConsentEndpointWithBearerToken;
import net.openid.conformance.condition.client.FAPIBrazilSignPaymentConsentRequest;
import net.openid.conformance.condition.client.GetResourceEndpointConfiguration;
import net.openid.conformance.condition.client.SetPaymentsScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalBrazilDCR;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsurePaymentDateIsToday;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideClientWithPagtoClient;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideClientWithPagtoClientThatHasClientSpecificJwks;
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

		eventLog.startBlock("Trying using a different client's MTLS certificate with issued access token");

		env.putObject("altconfig", env.getObject("config").deepCopy());
		env.mapKey("config", "altconfig");
		env.mapKey("mutual_tls_authentication", "altmtls");
		callAndStopOnFailure(OverrideClientWithPagtoClientThatHasClientSpecificJwks.class);
		callAndStopOnFailure(ExtractMTLSCertificatesFromConfiguration.class);

		call(exec().mapKey("request_object_claims", "consent_endpoint_request"));
		callAndStopOnFailure(AddJtiAsUuidToRequestObject.class, "BrazilOB-6.1");
		callAndStopOnFailure(AddIatToRequestObject.class, "BrazilOB-6.1");
		call(exec().unmapKey("request_object_claims"));
		callAndStopOnFailure(FAPIBrazilSignPaymentConsentRequest.class);
		callAndStopOnFailure(FAPIBrazilCallPaymentConsentEndpointWithBearerToken.class);

		call(exec().mapKey("endpoint_response", "consent_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, Condition.ConditionResult.FAILURE);

		env.unmapKey("config");

		eventLog.startBlock("Trying using a different client's MTLS certificate to authenticate at token endpoint");

		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
		callAndStopOnFailure(SetPaymentsScopeOnTokenEndpointRequest.class);
		call(sequence(addTokenEndpointClientAuthentication));
		callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class);
		callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckTokenEndpointHttpStatus400or401.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidClient.class, Condition.ConditionResult.FAILURE);

		env.unmapKey("mutual_tls_authentication");

		eventLog.startBlock("Delete client");

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
