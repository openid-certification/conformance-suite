package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantApplicableOnly;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-pid-claims-path-object-member",
	displayName = "OID4VP-1.0-FINAL: DCQL query selecting one member of a PID object claim",
	summary = """
		Requests a single member of the PID 'place_of_birth' object with a two-element claims path \
		pointer (OID4VP section 7.1). The PID Rulebook only guarantees that at least one of 'locality', \
		'region' and 'country' is present, so the three are offered as claim_sets alternatives in that \
		order.

		The wallet must return the members of exactly one of the options: separately disclosing members \
		from more than one option is a failure. If the issuer made the object disclosable only as a \
		whole, presenting the whole object is correct. The wallet should return the first option it can \
		satisfy. The suite cannot see which members the credential contains, so returning an option other \
		than 'locality' results in a warning; that warning can be ignored if the credential's place of \
		birth does not contain the more preferred members.

		Only applicable to the EUDI PID in SD-JWT VC format.""",
	profile = "OID4VP-1FINAL"
)
// exists only for the PID's 'nationalities' and 'place_of_birth' claims, in the one format where parts of them can be selected
@VariantApplicableOnly(parameter = VP1FinalWalletCredentialType.class, values = {"eudi_pid"})
@VariantApplicableOnly(parameter = VP1FinalWalletCredentialFormat.class, values = {"sd_jwt_vc"})
public class VP1FinalWalletPidObjectMember extends AbstractVP1FinalWalletTest {

	@Override
	protected String builtInDcqlResource() {
		return "/json/dcql/vp1final-wallet-eudi-pid-place-of-birth-member.json";
	}
}
