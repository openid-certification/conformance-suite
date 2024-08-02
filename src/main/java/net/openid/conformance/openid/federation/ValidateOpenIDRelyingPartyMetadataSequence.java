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
import net.openid.conformance.condition.as.dynregistration.ValidateInitiateLoginUri;
import net.openid.conformance.condition.as.dynregistration.ValidateRequireAuthTime;
import net.openid.conformance.condition.as.dynregistration.ValidateUserinfoSignedResponseAlg;
import net.openid.conformance.sequence.AbstractConditionSequence;

// This mostly correlates to validateClientRegistrationMetadata() methods found in various FAPI classes,
// with the Brazil conditions removed and with the remaining skipIfElementMissing parts reworked (or rather, commented out right now)
public class ValidateOpenIDRelyingPartyMetadataSequence extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		//check response type - grant type consistency
		callAndContinueOnFailure(ValidateClientGrantTypes.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		//basic checks like fragments, https etc
		callAndContinueOnFailure(OIDCCValidateClientRedirectUris.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		//check if logo is image
		callAndContinueOnFailure(ValidateClientLogoUris.class, Condition.ConditionResult.FAILURE,"OIDCR-2");
		//check if uri is valid
		callAndContinueOnFailure(ValidateClientUris.class, Condition.ConditionResult.FAILURE,"OIDCR-2");
		//check if uri is valid
		callAndContinueOnFailure(ValidateClientPolicyUris.class, Condition.ConditionResult.FAILURE,"OIDCR-2");
		//check if uri is valid
		callAndContinueOnFailure(ValidateClientTosUris.class, Condition.ConditionResult.FAILURE,"OIDCR-2");

		callAndContinueOnFailure(ValidateClientSubjectType.class, Condition.ConditionResult.FAILURE,"OIDCR-2");

		callAndContinueOnFailure(EnsureIdTokenEncryptedResponseAlgIsSetIfEncIsSet.class, Condition.ConditionResult.FAILURE,"OIDCR-2");

		//userinfo
		call(condition(ValidateUserinfoSignedResponseAlg.class)
			.skipIfElementMissing("client", "userinfo_signed_response_alg")
			.onSkip(Condition.ConditionResult.INFO)
			.requirements("OIDCR-2")
			.onFail(Condition.ConditionResult.FAILURE)
			.dontStopOnFailure());

		callAndContinueOnFailure(EnsureUserinfoEncryptedResponseAlgIsSetIfEncIsSet.class, Condition.ConditionResult.FAILURE,"OIDCR-2");

		//request object
		callAndContinueOnFailure(EnsureRequestObjectEncryptionAlgIsSetIfEncIsSet.class, Condition.ConditionResult.FAILURE,"OIDCR-2");

		callAndContinueOnFailure(ValidateDefaultMaxAge.class, Condition.ConditionResult.WARNING,"OIDCR-2");

		call(condition(ValidateRequireAuthTime.class)
			.skipIfElementMissing("client", "require_auth_time")
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
	}
}
