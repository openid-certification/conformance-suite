package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.dynregistration.CMFChileEnsureRegistrationRequestContainsMandatoryMetadata;
import net.openid.conformance.condition.as.dynregistration.CMFChileEnsureRegistrationRequestGrantTypesIsAuthorizationCodeOnly;
import net.openid.conformance.condition.as.dynregistration.CMFChileFetchDirectorySSAJwks;
import net.openid.conformance.condition.as.dynregistration.CMFChileValidateRegistrationRequestApplicationType;
import net.openid.conformance.condition.as.dynregistration.EnsureDynamicRegistrationRequestContainsSoftwareStatement;
import net.openid.conformance.condition.as.dynregistration.EnsureRegistrationRequestContainsAtLeastOneContact;
import net.openid.conformance.condition.as.dynregistration.EnsureSoftwareStatementContainsJti;
import net.openid.conformance.condition.as.dynregistration.EnsureTokenEndPointAuthMethodIsTlsClientAuth;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilExtractSSAFromDynamicRegistrationRequest;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilExtractSoftwareStatement;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateIdTokenSignedResponseAlg;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateRequestObjectSigningAlg;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateSSASignature;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateUserinfoSignedResponseAlg;
import net.openid.conformance.condition.as.dynregistration.ValidateSoftwareStatementExp;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

/**
 * Profile behavior for Chile Open Finance client tests.
 * Requires mTLS everywhere, and validates dynamic client registration requests and their software
 * statements against CMF Dynamic Client Registration; otherwise uses base-class defaults.
 */
public class OpenBankingChileClientProfileBehavior extends FAPI2ClientProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public ConditionSequence validateSoftwareStatement() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(EnsureDynamicRegistrationRequestContainsSoftwareStatement.class, "CMF-DCR-5");
				callAndStopOnFailure(FAPIBrazilExtractSSAFromDynamicRegistrationRequest.class);
				callAndStopOnFailure(CMFChileFetchDirectorySSAJwks.class, "CMF-DCR-5");
				callAndStopOnFailure(FAPIBrazilValidateSSASignature.class, "CMF-DCR-5");
				callAndStopOnFailure(FAPIBrazilExtractSoftwareStatement.class);
				callAndContinueOnFailure(ValidateSoftwareStatementExp.class, ConditionResult.FAILURE, "CMF-DCR-5");
				callAndContinueOnFailure(EnsureSoftwareStatementContainsJti.class, ConditionResult.FAILURE, "CMF-DCR-5");
			}
		};
	}

	@Override
	public ConditionSequence validateDynamicRegistrationRequest() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(CMFChileEnsureRegistrationRequestContainsMandatoryMetadata.class, ConditionResult.FAILURE, "CMF-DCR-6");
				callAndContinueOnFailure(EnsureRegistrationRequestContainsAtLeastOneContact.class, ConditionResult.FAILURE, "CMF-DCR-6");
				callAndContinueOnFailure(CMFChileEnsureRegistrationRequestGrantTypesIsAuthorizationCodeOnly.class, ConditionResult.FAILURE, "CMF-DCR-6");
				callAndContinueOnFailure(CMFChileValidateRegistrationRequestApplicationType.class, ConditionResult.FAILURE, "CMF-DCR-6");
				call(exec().mapKey("client", "dynamic_registration_request"));
				callAndContinueOnFailure(EnsureTokenEndPointAuthMethodIsTlsClientAuth.class, ConditionResult.FAILURE, "CMF-DCR-6", "CMF-DCR-7");
				call(exec().unmapKey("client"));
			}
		};
	}

	@Override
	public ConditionSequence validateDynamicRegistrationSigningAlgs() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(FAPIBrazilValidateIdTokenSignedResponseAlg.class)
					.skipIfElementMissing("client", "id_token_signed_response_alg")
					.onSkip(ConditionResult.INFO).onFail(ConditionResult.FAILURE).dontStopOnFailure()
					.requirement("CMF-DCR-7"));
				call(condition(FAPIBrazilValidateUserinfoSignedResponseAlg.class)
					.skipIfElementMissing("client", "userinfo_signed_response_alg")
					.onSkip(ConditionResult.INFO).onFail(ConditionResult.FAILURE).dontStopOnFailure()
					.requirement("CMF-DCR-7"));
				call(condition(FAPIBrazilValidateRequestObjectSigningAlg.class)
					.skipIfElementMissing("client", "request_object_signing_alg")
					.onSkip(ConditionResult.INFO).onFail(ConditionResult.FAILURE).dontStopOnFailure()
					.requirement("CMF-DCR-7"));
			}
		};
	}

	@Override
	public String dynamicRegistrationRequiresMtlsMessage() {
		return "Dynamic client registration requests must be performed over HTTPS with mutual TLS, as per CMF Dynamic Client Registration section 7";
	}
}
