package net.openid.conformance.openinsurance.testmodule.support;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckItemCountHasMin1;
import net.openid.conformance.condition.client.CreateEmptyResourceEndpointRequestHeaders;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import net.openid.conformance.condition.client.FAPIBrazilAddConsentIdToClientScope;
import net.openid.conformance.condition.client.FAPIBrazilAddExpirationToConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilCreateConsentRequest;
import net.openid.conformance.condition.client.ValidateExpiresIn;
import net.openid.conformance.openbanking_brasil.testmodules.support.CallConsentApiWithBearerToken;
import net.openid.conformance.openbanking_brasil.testmodules.support.ConsentIdExtractor;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseHasLinks;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToFetchConsentRequest;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToPostConsentRequest;
import net.openid.conformance.openbanking_brasil.testmodules.support.RemoveConsentScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.SaveConsentsAccessToken;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetContentTypeApplicationJson;
import net.openid.conformance.openbanking_brasil.testmodules.support.ValidateResponseMetaData;
import net.openid.conformance.openinsurance.validator.consents.v1.OpinConsentDetailsIdentifiedByConsentIdValidatorV1;
import net.openid.conformance.openinsurance.validator.consents.v1.OpinCreateNewConsentValidatorV1;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

public class OpinPreAuthorizationConsentApi extends AbstractConditionSequence {

	private Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest;

	public OpinPreAuthorizationConsentApi(Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest) {
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

		call(exec().startBlock("Validating create consent response v1"));
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
		callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
		callAndStopOnFailure(SetContentTypeApplicationJson.class);
		callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(OpinCreateNewConsentValidatorV1.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.REVIEW);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.REVIEW);
		callAndContinueOnFailure(CheckItemCountHasMin1.class);

		call(exec().startBlock("Validating get consent response v1"));
		callAndStopOnFailure(ConsentIdExtractor.class);
		callAndStopOnFailure(PrepareToFetchConsentRequest.class);
		callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(OpinConsentDetailsIdentifiedByConsentIdValidatorV1.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.REVIEW);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.REVIEW);
		callAndStopOnFailure(FAPIBrazilAddConsentIdToClientScope.class);

		callAndStopOnFailure(RemoveConsentScope.class);
	}
}
