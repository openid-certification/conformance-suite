package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-age-over-18",
	displayName = "OID4VP-1.0-FINAL: Request proof that the holder is aged 18 or over",
	summary = """
		Performs the normal flow, but with a DCQL query a verifier would use to check the holder \
		is aged 18 or over. The query uses claim_sets to offer, in order of preference, the \
		age_over_18, age_in_years, age_birth_year and birth_date data elements, so the elements \
		that reveal the least about the holder are preferred and the date of birth is only a \
		last resort.

		The wallet must return the data elements of exactly one of the options: disclosing \
		elements from more than one option (for example both age_over_18 and birth_date) is a \
		failure. The wallet should return the first option it can satisfy. The suite cannot see \
		which data elements the credential contains, so returning any option other than \
		age_over_18 results in a warning; that warning can be ignored if the credential does not \
		contain the more preferred elements. age_over_18 is mandatory in a Photo ID and optional \
		in an mDL.

		Only applicable to the 'mdl' and 'photoid' credential types: the EUDI PID defines no age \
		attributes other than the date of birth.""",
	profile = "OID4VP-1FINAL"
)
@VariantNotApplicable(parameter = VP1FinalWalletCredentialType.class, values = {"custom", "eudi_pid"})
public class VP1FinalWalletAgeOver18 extends AbstractVP1FinalWalletTest {

	@Override
	protected String builtInDcqlResource() {
		return credentialType.getAgeOver18DcqlResource(credentialFormat);
	}
}
