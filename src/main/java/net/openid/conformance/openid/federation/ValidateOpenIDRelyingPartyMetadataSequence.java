package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.dynregistration.EnsureIdTokenEncryptedResponseAlgIsSetIfEncIsSet;
import net.openid.conformance.condition.as.dynregistration.EnsureRequestObjectEncryptionAlgIsSetIfEncIsSet;
import net.openid.conformance.condition.as.dynregistration.EnsureUserinfoEncryptedResponseAlgIsSetIfEncIsSet;
import net.openid.conformance.condition.as.dynregistration.OIDCCValidateClientRedirectUris;
import net.openid.conformance.condition.as.dynregistration.ValidateClientGrantTypes;
import net.openid.conformance.condition.as.dynregistration.ValidateClientLogoUris;
import net.openid.conformance.condition.as.dynregistration.ValidateClientPolicyUris;
import net.openid.conformance.condition.as.dynregistration.ValidateClientSubjectType;
import net.openid.conformance.condition.as.dynregistration.ValidateClientTosUris;
import net.openid.conformance.condition.as.dynregistration.ValidateClientUris;
import net.openid.conformance.condition.as.dynregistration.ValidateDefaultAcrValues;
import net.openid.conformance.condition.as.dynregistration.ValidateDefaultMaxAge;
import net.openid.conformance.condition.as.dynregistration.ValidateIdTokenSignedResponseAlg;
import net.openid.conformance.condition.as.dynregistration.ValidateInitiateLoginUri;
import net.openid.conformance.condition.as.dynregistration.ValidateRequestObjectSigningAlg;
import net.openid.conformance.condition.as.dynregistration.ValidateRequestUris;
import net.openid.conformance.condition.as.dynregistration.ValidateRequireAuthTime;
import net.openid.conformance.condition.as.dynregistration.ValidateTokenEndpointAuthSigningAlg;
import net.openid.conformance.condition.as.dynregistration.ValidateUserinfoSignedResponseAlg;
import net.openid.conformance.sequence.AbstractConditionSequence;

// This corresponds to AbstractOIDCCClientTest.validateClientMetadata()
public class ValidateOpenIDRelyingPartyMetadataSequence extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		//check response type - grant type consistency
		callAndContinueOnFailure(ValidateClientGrantTypes.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		//basic checks like fragments, https etc
		callAndContinueOnFailure(OIDCCValidateClientRedirectUris.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		//check if logo is image
		callAndContinueOnFailure(ValidateClientLogoUris.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
		//check if uri is valid
		callAndContinueOnFailure(ValidateClientUris.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
		//check if uri is valid
		callAndContinueOnFailure(ValidateClientPolicyUris.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
		//check if uri is valid
		callAndContinueOnFailure(ValidateClientTosUris.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		callAndContinueOnFailure(ValidateClientSubjectType.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		call(condition(ValidateIdTokenSignedResponseAlg.class)
			.skipIfElementMissing("client", "id_token_signed_response_alg")
			.onSkip(Condition.ConditionResult.INFO)
			.requirements("OIDCR-2")
			.onFail(Condition.ConditionResult.FAILURE)
			.dontStopOnFailure());

		callAndContinueOnFailure(EnsureIdTokenEncryptedResponseAlgIsSetIfEncIsSet.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		//userinfo
		call(condition(ValidateUserinfoSignedResponseAlg.class)
			.skipIfElementMissing("client", "userinfo_signed_response_alg")
			.onSkip(Condition.ConditionResult.INFO)
			.requirements("OIDCR-2")
			.onFail(Condition.ConditionResult.FAILURE)
			.dontStopOnFailure());
		callAndContinueOnFailure(EnsureUserinfoEncryptedResponseAlgIsSetIfEncIsSet.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		//request object
		call(condition(ValidateRequestObjectSigningAlg.class)
			.skipIfElementMissing("client", "request_object_signing_alg")
			.onSkip(Condition.ConditionResult.INFO)
			.requirements("OIDCR-2")
			.onFail(Condition.ConditionResult.FAILURE)
			.dontStopOnFailure());
		callAndContinueOnFailure(EnsureRequestObjectEncryptionAlgIsSetIfEncIsSet.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		call(condition(ValidateTokenEndpointAuthSigningAlg.class)
			.skipIfElementMissing("client", "token_endpoint_auth_signing_alg")
			.onSkip(Condition.ConditionResult.INFO)
			.requirements("OIDCR-2")
			.onFail(Condition.ConditionResult.FAILURE)
			.dontStopOnFailure());

		callAndContinueOnFailure(ValidateDefaultMaxAge.class, Condition.ConditionResult.WARNING, "OIDCR-2");

		call(condition(ValidateRequireAuthTime.class)
			.skipIfElementMissing("client", "require_auth_time")
			.onSkip(Condition.ConditionResult.INFO)
			.requirements("OIDCR-2")
			.onFail(Condition.ConditionResult.FAILURE)
			.dontStopOnFailure());

		call(condition(ValidateDefaultAcrValues.class)
			.skipIfElementMissing("client", "default_acr_values")
			.onSkip(Condition.ConditionResult.INFO)
			.requirements("OIDCR-2")
			.onFail(Condition.ConditionResult.FAILURE)
			.dontStopOnFailure());

		call(condition(ValidateInitiateLoginUri.class)
			.skipIfElementMissing("client", "initiate_login_uri")
			.onSkip(Condition.ConditionResult.INFO)
			.requirements("OIDCR-2")
			.onFail(Condition.ConditionResult.FAILURE)
			.dontStopOnFailure());

		call(condition(ValidateRequestUris.class)
			.skipIfElementMissing("client", "request_uris")
			.onSkip(Condition.ConditionResult.INFO)
			.requirements("OIDCR-2")
			.onFail(Condition.ConditionResult.FAILURE)
			.dontStopOnFailure());
	}

}
