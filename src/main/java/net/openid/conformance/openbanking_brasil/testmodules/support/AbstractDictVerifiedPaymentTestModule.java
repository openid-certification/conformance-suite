package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages.TestTimedOut;
import net.openid.conformance.sequence.ConditionSequence;

import java.util.Optional;

public abstract class AbstractDictVerifiedPaymentTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void validateClientConfiguration() {
		callAndStopOnFailure(AddPaymentScope.class);
		super.validateClientConfiguration();
	}


	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		eventLog.log(getName(), "Payments scope present - protected resource assumed to be a payments endpoint");
		OpenBankingBrazilPreAuthorizationErrorAgnosticSteps steps = new OpenBankingBrazilPreAuthorizationErrorAgnosticSteps(addTokenEndpointClientAuthentication);
		consentErrorMessageCondition().ifPresent(c -> {
			steps.insertAfter(OptionallyAllow201Or422.class, condition(c));
		});
		return steps;
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		env.putBoolean("consent_rejected", false);
		eventLog.startBlock("Setting date to today");
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
		callAndStopOnFailure(RemoveTransactionIdentification.class);
		configureDictInfo();
	}

	protected abstract void configureDictInfo();

	protected Optional<Class<? extends Condition>> resourceCreationErrorMessageCondition() {
		return Optional.empty();
	}

	protected Optional<Class<? extends Condition>> consentErrorMessageCondition() {
		return Optional.empty();
	}

	@Override
	protected void requestProtectedResource() {
		if(!validationStarted) {
			validationStarted = true;
			ConditionSequence pixSequence = new CallPixPaymentsEndpointSequence()
				.replace(CallProtectedResourceWithBearerTokenAndCustomHeaders.class,
					condition(CallProtectedResourceWithBearerTokenAndCustomHeadersOptionalError.class))
				.skip(EnsureHttpStatusCodeIs201.class, "Skipping 201 check");
			resourceCreationErrorMessageCondition().ifPresent(c -> {
				pixSequence.insertAfter(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, condition(c));
			});
			call(pixSequence);
			eventLog.startBlock(currentClientString() + "Validate response");
			validateResponse();
			eventLog.endBlock();
		}
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
	protected void validateResponse() {
		callAndStopOnFailure(ProxyTestCheckForPass.class);
		callAndStopOnFailure(EnsureProxyTestResourceResponseCodeWas422.class);
		resourceCreationErrorMessageCondition().ifPresent(c -> {
			callAndStopOnFailure(c);
		});

		if (!env.getBoolean("proxy_payment_422")) {
			int count = 1;
			boolean keepPolling = true;
			while (keepPolling) {
				callAndStopOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
				callAndStopOnFailure(EnsureSelfLinkEndsInPaymentId.class, Condition.ConditionResult.FAILURE);
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
				call(statusValidationSequence());

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

	protected ConditionSequence statusValidationSequence() {
		return sequenceOf(
			condition(PaymentsProxyCheckForRejectedStatus.class),
			condition(PaymentsProxyCheckForInvalidStatus.class));
	}

}
