package net.openid.conformance.vp1finalwallet;

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

		Not applicable to the 'custom' credential type, as the mandatory data element set of a \
		custom credential is not known to the suite.""",
	profile = "OID4VP-1FINAL"
)
@VariantNotApplicable(parameter = VP1FinalWalletCredentialType.class, values = {"custom"})
public class VP1FinalWalletAllMandatoryClaims extends AbstractVP1FinalWalletTest {

	@Override
	protected String builtInDcqlResource(VP1FinalWalletCredentialType type) {
		return type.getAllMandatoryClaimsDcqlResource();
	}
}
