package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.dynregistration.CMFChileFetchDirectorySSAJwks;
import net.openid.conformance.condition.as.dynregistration.EnsureDynamicRegistrationRequestContainsSoftwareStatement;
import net.openid.conformance.condition.as.dynregistration.EnsureSoftwareStatementContainsJti;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilExtractSSAFromDynamicRegistrationRequest;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilExtractSoftwareStatement;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateSSASignature;
import net.openid.conformance.condition.as.dynregistration.ValidateSoftwareStatementExp;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

/**
 * Profile behavior for Chile Open Finance client tests.
 * Requires mTLS everywhere, and validates dynamic client registration software statements
 * against the CMF Directorio; otherwise uses base-class defaults.
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
	public String dynamicRegistrationRequiresMtlsMessage() {
		return "Dynamic client registration requests must be performed over HTTPS with mutual TLS, as per CMF Dynamic Client Registration section 7";
	}
}
