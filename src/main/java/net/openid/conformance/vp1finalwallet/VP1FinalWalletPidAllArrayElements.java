package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.CheckSdJwtArrayElementDisclosures;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantApplicableOnly;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-pid-claims-path-array-all-elements",
	displayName = "OID4VP-1.0-FINAL: DCQL query selecting all elements of a PID array claim",
	summary = """
		Requests all elements of the PID 'nationalities' claim with the claims path pointer \
		['nationalities', null] (OID4VP section 7.1). The wallet must return the credential with the \
		'nationalities' array disclosed with at least one element. The suite cannot tell an element the \
		wallet withheld from a decoy digest added by the issuer, so array placeholders without a disclosure \
		are reported in the log but are not a failure.
		Only applicable to the EUDI PID in SD-JWT VC format.""",
	profile = "OID4VP-1FINAL"
)
// exists only for the PID's 'nationalities' and 'place_of_birth' claims, in the one format where parts of them can be selected
@VariantApplicableOnly(parameter = VP1FinalWalletCredentialType.class, values = {"eudi_pid"})
@VariantApplicableOnly(parameter = VP1FinalWalletCredentialFormat.class, values = {"sd_jwt_vc"})
public class VP1FinalWalletPidAllArrayElements extends AbstractVP1FinalWalletTest {

	@Override
	protected String builtInDcqlResource() {
		return "/json/dcql/vp1final-wallet-eudi-pid-nationalities-all.json";
	}

	@Override
	protected void validateDisclosedClaimsMatchDcqlQuery() {
		// the shared check drops the null selector and so verifies that 'nationalities' is present
		super.validateDisclosedClaimsMatchDcqlQuery();
		callAndContinueOnFailure(CheckSdJwtArrayElementDisclosures.class, ConditionResult.FAILURE,
			"OID4VP-1FINAL-7.1", "OID4VP-1FINAL-6.4");
	}
}
