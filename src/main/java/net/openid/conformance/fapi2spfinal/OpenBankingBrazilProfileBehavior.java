package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.FAPIBrazilValidateExpiresIn;
import net.openid.conformance.condition.client.FAPIBrazilValidateIdTokenSigningAlg;
import net.openid.conformance.condition.common.FAPIBrazilCheckKeyAlgInClientJWKs;
import net.openid.conformance.sequence.ConditionSequence;

import java.util.function.Supplier;

/**
 * OpenBanking Brazil profile behavior.
 */
public class OpenBankingBrazilProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public Supplier<? extends ConditionSequence> preAuthorizationSteps(
		AbstractFAPI2SPFinalServerTestModule module) {
		return module::createOBBPreauthSteps;
	}

	@Override
	public void validateKeyAlgorithms(AbstractFAPI2SPFinalServerTestModule module) {
		module.doCallAndContinueOnFailure(FAPIBrazilCheckKeyAlgInClientJWKs.class, ConditionResult.FAILURE, "BrazilOB-6.1");
	}

	@Override
	public boolean encryptRequestObject(boolean isPar) {
		return !isPar;
	}

	@Override
	public void validateTokenExpiresIn(AbstractFAPI2SPFinalServerTestModule module) {
		module.doSkipIfMissing(new String[]{"expires_in"}, null, ConditionResult.INFO,
			FAPIBrazilValidateExpiresIn.class, ConditionResult.FAILURE, "BrazilOB-5.2.2-12");
	}

	@Override
	public void validateIdTokenSigningAlg(AbstractFAPI2SPFinalServerTestModule module) {
		module.doCallAndContinueOnFailure(FAPIBrazilValidateIdTokenSigningAlg.class, ConditionResult.FAILURE, "BrazilOB-6.1");
	}

	@Override
	public void setupResourceRequestBody(AbstractFAPI2SPFinalServerTestModule module) {
		module.setupBrazilPaymentsResourceRequestBody();
	}

	@Override
	public void validateResourceResponseBody(AbstractFAPI2SPFinalServerTestModule module) {
		module.validateBrazilPaymentInitiationSignedResponse();
	}
}
