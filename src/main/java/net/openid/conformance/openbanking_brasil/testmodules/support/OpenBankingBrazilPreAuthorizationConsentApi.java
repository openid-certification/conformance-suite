package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.consent.ConsentDetailsIdentifiedByConsentIdValidator;
import net.openid.conformance.openbanking_brasil.consent.CreateNewConsentValidator;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.OIDFJSON;

public class OpenBankingBrazilPreAuthorizationConsentApi extends AbstractConditionSequence {

	private Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest;

	public OpenBankingBrazilPreAuthorizationConsentApi(Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest) {
		this.addClientAuthenticationToTokenEndpointRequest = addClientAuthenticationToTokenEndpointRequest;
	}

	@Override
	public void evaluate() {

		call(exec().startBlock("Use client_credentials grant to obtain Brazil consent"));

		/* create client credentials request */

		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);

		call(sequence(addClientAuthenticationToTokenEndpointRequest));

		/* get access token */

		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class);

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class, "RFC6749-4.4.3", "RFC6749-5.1");

		call(condition(ValidateExpiresIn.class)
			.skipIfObjectMissing("expires_in")
			.onSkip(Condition.ConditionResult.INFO)
			.requirements("RFC6749-5.1")
			.onFail(Condition.ConditionResult.FAILURE)
			.dontStopOnFailure());

		callAndContinueOnFailure(SaveConsentsAccessToken.class);

		/* create consent request */

		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);

		callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class);

		call(exec().startBlock("Validating create consent response"));
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
		callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
		callAndStopOnFailure(SetContentTypeApplicationJson.class);
		callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CreateNewConsentValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.REVIEW);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.REVIEW);
		callAndContinueOnFailure(CheckItemCountHasMin1.class);

		call(exec().startBlock("Validating get consent response"));
		callAndStopOnFailure(ConsentIdExtractor.class);
		callAndStopOnFailure(PrepareToFetchConsentRequest.class);
		callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ConsentDetailsIdentifiedByConsentIdValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.REVIEW);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.REVIEW);
		callAndStopOnFailure(FAPIBrazilAddConsentIdToClientScope.class);

		callAndStopOnFailure(RemoveConsentScope.class);
	}
}
