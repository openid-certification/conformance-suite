package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddAuthorizationCodeGrantTypeToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddClientNameToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddContactsToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddImplicitGrantTypeToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddPublicJwksToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddRedirectUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddResponseTypesArrayToDynamicRegistrationRequestFromEnvironment;
import net.openid.conformance.condition.client.AddTokenEndpointAuthMethodToDynamicRegistrationRequestFromEnvironment;
import net.openid.conformance.condition.client.CreateEmptyDynamicRegistrationRequest;
import net.openid.conformance.condition.client.GenerateRS256ClientJWKs;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.variant.ResponseType;

public class OIDCCCreateDynamicClientRegistrationRequest extends AbstractConditionSequence {

	private final ResponseType responseType;

	public OIDCCCreateDynamicClientRegistrationRequest(ResponseType responseType) {
		this.responseType = responseType;
	}

	@Override
	public void evaluate() {
		callAndStopOnFailure(GenerateRS256ClientJWKs.class);

		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, Condition.ConditionResult.FAILURE, "RFC7517-4.5");

		// create basic dynamic registration request
		callAndStopOnFailure(CreateEmptyDynamicRegistrationRequest.class);
		callAndStopOnFailure(AddClientNameToDynamicRegistrationRequest.class);

		if (responseType.includesCode()) {
			callAndStopOnFailure(AddAuthorizationCodeGrantTypeToDynamicRegistrationRequest.class);
		}

		if (responseType.includesIdToken() || responseType.includesToken()) {
			callAndStopOnFailure(AddImplicitGrantTypeToDynamicRegistrationRequest.class);
		}

		callAndStopOnFailure(AddPublicJwksToDynamicRegistrationRequest.class, "RFC7591-2");
		callAndStopOnFailure(AddTokenEndpointAuthMethodToDynamicRegistrationRequestFromEnvironment.class);
		callAndStopOnFailure(AddResponseTypesArrayToDynamicRegistrationRequestFromEnvironment.class);
		callAndStopOnFailure(AddRedirectUriToDynamicRegistrationRequest.class);

		callAndContinueOnFailure(AddContactsToDynamicRegistrationRequest.class, Condition.ConditionResult.INFO);
	}

}
