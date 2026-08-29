package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.EnsurePidPictureClaimDisclosed;
import net.openid.conformance.condition.client.ValidateDisclosedClaimsMatchDcqlQueryExceptPicture;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-all-mandatory-claims",
	displayName = "OID4VP-1.0-FINAL: Request all mandatory claims of the credential type",
	summary = """
		Performs the normal flow, but requests every mandatory data element of the selected \
		credential type (as defined by ISO/IEC 18013-5 for mDL, ISO/IEC TS 23220-4 for Photo ID \
		and the EUDI PID Rulebook for the PID) instead of the suite's minimal default query. The \
		wallet should disclose all of the requested claims, which a conformant credential is \
		guaranteed to contain.

		For the PID, the portrait (picture claim) is requested via a preferred claim set, with a \
		fallback claim set omitting it, so a wallet whose credential has no portrait can still \
		satisfy the query. Its absence from the presented credential is reported as a warning: \
		mandatory inclusion of the portrait only applies 24 months after entry into force of the \
		regulation amending CIR 2024/2977, and users may opt out of it, so a conformant \
		credential is not guaranteed to contain it.

		Not applicable to the 'custom' credential type, as the mandatory data element set of a \
		custom credential is not known to the suite.""",
	profile = "OID4VP-1FINAL"
)
@VariantNotApplicable(parameter = VP1FinalWalletCredentialType.class, values = {"custom"})
public class VP1FinalWalletAllMandatoryClaims extends AbstractVP1FinalWalletTest {

	@Override
	protected String builtInDcqlResource() {
		return credentialType.getAllMandatoryClaimsDcqlResource();
	}

	@Override
	protected void validateDisclosedClaimsMatchDcqlQuery() {
		if (credentialType == VP1FinalWalletCredentialType.EUDI_PID) {
			callAndContinueOnFailure(ValidateDisclosedClaimsMatchDcqlQueryExceptPicture.class,
				ConditionResult.FAILURE, "OID4VP-1FINAL-6.4.1");
			callAndContinueOnFailure(EnsurePidPictureClaimDisclosed.class,
				ConditionResult.WARNING, "PIDRULEBOOK-2.2");
		} else {
			super.validateDisclosedClaimsMatchDcqlQuery();
		}
	}
}
