package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages.TestTimedOut;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-proxy-fake-email-proxy-test",
	displayName = "Payments API test module ensuring email address is incorrect",
	summary = "Payments API test module ensuring email address is incorrect" +
		"Flow:" +
		"Makes a bad DICT payment flow with an incorrect email - expects a 422." +
		"Required:" +
		"Consent url pointing at the consent endpoint." +
		"Config: Debtor account must be present in the config. We manually set the local instrument to DICT, add a creditor account, add an fake email address.",
	profile = OBBProfile.OBB_PROFILE,
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
public class PaymentsApiWrongEmailAddressProxyTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void validateClientConfiguration() {
		callAndStopOnFailure(AddPaymentScope.class);
		super.validateClientConfiguration();
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		eventLog.log(getName(), "Payments scope present - protected resource assumed to be a payments endpoint");
		OpenBankingBrazilPreAuthorizationErrorAgnosticSteps steps = new OpenBankingBrazilPreAuthorizationErrorAgnosticSteps(addTokenEndpointClientAuthentication);
		return steps;
	}

	@Override
	protected void performPreAuthorizationSteps() {
		super.performPreAuthorizationSteps();
		if(env.getString("proceed_with_test") == null) {
			eventLog.log(getName(), "Consent call failed early - test finished");
			fireTestFinished();
		}
	}

	@Override
	protected void requestProtectedResource() {
		if(!validationStarted) {
			validationStarted = true;
			call(new CallPixPaymentsEndpointSequence()
				.replace(CallProtectedResourceWithBearerTokenAndCustomHeaders.class,
					condition(CallProtectedResourceWithBearerTokenAndCustomHeadersOptionalError.class))
				.skip(EnsureHttpStatusCodeIs201.class, "Skipping 201 check")
			);
			eventLog.startBlock(currentClientString() + "Validate response");
			validateResponse();
			eventLog.endBlock();
		}
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		eventLog.startBlock("Setting date to today");
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		callAndContinueOnFailure(SelectDICTCodeLocalInstrument.class);
		callAndContinueOnFailure(SelectDICTCodePixLocalInstrument.class);
		callAndContinueOnFailure(RemoveQRCodeFromConfig.class);
		callAndContinueOnFailure(InjectRealCreditorAccountToPaymentConsent.class);
		callAndContinueOnFailure(InjectRealCreditorAccountToPayment.class);
		callAndContinueOnFailure(SetProxyToFakeEmailAddressOnPaymentConsent.class);
		callAndContinueOnFailure(SetProxyToFakeEmailAddressOnPayment.class);

		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
	}

	@Override
	protected void validateResponse() {
		callAndStopOnFailure(ProxyTestCheckForPass.class);
		callAndStopOnFailure(EnsureProxyTestResourceResponseCodeWas422.class);

		if (!env.getBoolean("proxy_payment_422")) {
			int count = 1;
			boolean keepPolling = true;
			while (keepPolling) {
				callAndStopOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
				callAndStopOnFailure(WaitFor30Seconds.class);
				call(new ValidateSelfEndpoint()
					.replace(CallProtectedResourceWithBearerToken.class, sequenceOf(
						condition(AddJWTAcceptHeader.class),
						condition(CallProtectedResourceWithBearerTokenAndCustomHeaders.class)
					))
					.skip(SaveOldValues.class, "Not saving old values")
					.skip(LoadOldValues.class, "Not loading old values")
				);
				callAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");
				callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");
				callAndStopOnFailure(EnsureMatchingFAPIInteractionId.class);
				call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
				call(exec().mapKey("endpoint_response_jwt", "consent_endpoint_response_jwt"));
				callAndStopOnFailure(EnsureContentTypeApplicationJwt.class);
				callAndStopOnFailure(ExtractSignedJwtFromResourceResponse.class);
				callAndStopOnFailure(FAPIBrazilValidateResourceResponseSigningAlg.class);
				callAndStopOnFailure(FAPIBrazilValidateResourceResponseTyp.class);
				call(exec().mapKey("server", "org_server"));
				call(exec().mapKey("server_jwks", "org_server_jwks"));
				callAndStopOnFailure(FetchServerKeys.class);
				call(exec().unmapKey("server"));
				call(exec().unmapKey("server_jwks"));
				callAndContinueOnFailure(ValidateResourceResponseSignature.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");
				callAndContinueOnFailure(ValidateResourceResponseJwtClaims.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");
				call(exec().unmapKey("endpoint_response"));
				call(exec().unmapKey("endpoint_response_jwt"));

				callAndContinueOnFailure(CheckPollStatus.class);
				callAndStopOnFailure(PaymentsProxyCheckForRejectedStatus.class);
				callAndStopOnFailure(PaymentsProxyCheckForInvalidStatus.class);

				if (env.getBoolean("payment_proxy_check_for_reject")) {
					if (env.getBoolean("consent_rejected")) {
						keepPolling = false;
					}
				}

				if (count >= 8) {
					keepPolling = false;
					callAndStopOnFailure(TestTimedOut.class);
					callAndStopOnFailure(ChuckWarning.class, Condition.ConditionResult.FAILURE);
				} else {
					count++;
				}
			}
		}
	}

}
