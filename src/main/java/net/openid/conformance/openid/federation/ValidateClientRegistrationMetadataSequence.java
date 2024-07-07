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
import net.openid.conformance.condition.as.dynregistration.ValidateDefaultMaxAge;
import net.openid.conformance.condition.as.dynregistration.ValidateUserinfoSignedResponseAlg;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateClientRegistrationMetadataSequence extends AbstractConditionSequence {

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

			/*
			skipIfElementMissing("client", "id_token_signed_response_alg", Condition.ConditionResult.INFO,
				FAPIBrazilValidateIdTokenSignedResponseAlg.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.2");
			*/

		callAndContinueOnFailure(EnsureIdTokenEncryptedResponseAlgIsSetIfEncIsSet.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		//userinfo
		call(condition(ValidateUserinfoSignedResponseAlg.class)
			.skipIfElementMissing("client", "userinfo_signed_response_alg")
			.onSkip(Condition.ConditionResult.INFO)
			.requirements("OIDCR-2")
			.onFail(Condition.ConditionResult.FAILURE)
			.dontStopOnFailure());
			/*
			skipIfElementMissing("client", "userinfo_signed_response_alg", Condition.ConditionResult.INFO,
				FAPIBrazilValidateUserinfoSignedResponseAlg.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.2");
			*/

		callAndContinueOnFailure(EnsureUserinfoEncryptedResponseAlgIsSetIfEncIsSet.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		//request object
			/*
			skipIfElementMissing("client", "request_object_signing_alg", Condition.ConditionResult.INFO,
				FAPIBrazilValidateRequestObjectSigningAlg.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
			*/
		callAndContinueOnFailure(EnsureRequestObjectEncryptionAlgIsSetIfEncIsSet.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

			/*
			skipIfElementMissing("client", "token_endpoint_auth_signing_alg", Condition.ConditionResult.INFO,
				FAPIBrazilValidateTokenEndpointAuthSigningAlg.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
			*/
		callAndContinueOnFailure(ValidateDefaultMaxAge.class, Condition.ConditionResult.WARNING, "OIDCR-2");

		call(condition(ValidateUserinfoSignedResponseAlg.class)
			.skipIfElementMissing("client", "require_auth_time")
			.onSkip(Condition.ConditionResult.INFO)
			.requirements("OIDCR-2")
			.onFail(Condition.ConditionResult.FAILURE)
			.dontStopOnFailure());
			/*
			skipIfElementMissing("client", "default_acr_values", Condition.ConditionResult.INFO,
				FAPIBrazilValidateDefaultAcrValues.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
			*/

		call(condition(ValidateUserinfoSignedResponseAlg.class)
			.skipIfElementMissing("client", "initiate_login_uri")
			.onSkip(Condition.ConditionResult.INFO)
			.requirements("OIDCR-2")
			.onFail(Condition.ConditionResult.FAILURE)
			.dontStopOnFailure());

		//TODO not allow request_uris?
			/*
			skipIfElementMissing("client", "request_uris", Condition.ConditionResult.INFO,
				ValidateRequestUris.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
			 */
	}

}
