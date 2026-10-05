package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.CheckSdJwtArrayElementDisclosures;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantApplicableOnly;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-pid-claims-path-array-index",
	displayName = "OID4VP-1.0-FINAL: DCQL query selecting the first element of a PID array claim",
	summary = """
		Requests the first element of the PID 'nationalities' claim with the claims path pointer \
		['nationalities', 0] (OID4VP section 7.1). Every PID has at least one nationality, so the wallet \
		must return the credential with the 'nationalities' array disclosed. If the issuer made the array \
		elements individually selectively disclosable, the wallet must not disclose any element other than \
		the selected one; if the array can only be disclosed as a whole, presenting the whole array is correct.

		The suite cannot tell whether the element presented really is the first one: a wallet that withholds \
		the first element and discloses a later one looks the same as an issuer that placed a decoy digest \
		first. This module therefore checks that nothing beyond one element is disclosed, not which one.

		Only applicable to the EUDI PID in SD-JWT VC format: an ISO mdoc always presents the entire array.""",
	profile = "OID4VP-1FINAL"
)
// exists only for the PID's 'nationalities' and 'place_of_birth' claims, in the one format where parts of them can be selected
@VariantApplicableOnly(parameter = VP1FinalWalletCredentialType.class, values = {"eudi_pid"})
@VariantApplicableOnly(parameter = VP1FinalWalletCredentialFormat.class, values = {"sd_jwt_vc"})
public class VP1FinalWalletPidArrayElementByIndex extends AbstractVP1FinalWalletTest {

	@Override
	protected String builtInDcqlResource() {
		return "/json/dcql/vp1final-wallet-eudi-pid-nationalities-first.json";
	}

	@Override
	protected void validateDisclosedClaimsMatchDcqlQuery() {
		// the shared check drops the index and so verifies that 'nationalities' is present
		super.validateDisclosedClaimsMatchDcqlQuery();
		callAndContinueOnFailure(CheckSdJwtArrayElementDisclosures.class, ConditionResult.FAILURE,
			"OID4VP-1FINAL-7.1", "OID4VP-1FINAL-6.4");
	}
}
