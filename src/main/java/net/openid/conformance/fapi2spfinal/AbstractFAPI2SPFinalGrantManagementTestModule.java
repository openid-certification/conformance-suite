package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.GrantManagement;
import net.openid.conformance.variant.VariantNotApplicable;

/**
 * Shared base for FAPI2-SP-Final OP grant management test modules.
 *
 * <p>Grant management certification is only meaningful for generic FAPI, where it is an opt-in
 * capability, and for Chile, whose profile requires it. Every other profile - including the client
 * credentials grant, which has no authorization flow to produce a grant at all - must not generate
 * these modules.
 *
 * <p>The static {@code @VariantNotApplicable} on {@link FAPI2FinalOPProfile} is what actually drops
 * these modules from those profiles. The conditional
 * {@code @VariantNotApplicableWhen(GrantManagement=enabled, ...)} on
 * {@link AbstractFAPI2SPFinalServerTestModule} - which is what hides the value in the UI dropdown - is
 * not sufficient on its own:
 * because these modules also mark {@code GrantManagement=disabled} not-applicable, the effective set
 * of allowed {@code GrantManagement} values collapses to empty under that profile, and
 * {@code VariantService.isApplicableForVariant} treats an empty effective set as "parameter not
 * applicable, skip" rather than dropping the module. Excluding the profile values directly gives a
 * non-empty effective set that simply does not contain the selected profile, so the module is
 * correctly dropped.
 */
@VariantNotApplicable(parameter = GrantManagement.class, values = {"disabled"})
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = {"openbanking_uk",
	"consumerdataright_au", "openbanking_brazil", "connectid_au", "cbuae", "ksa",
	"fapi_client_credentials_grant", "vci", "vci_haip"})
public abstract class AbstractFAPI2SPFinalGrantManagementTestModule extends AbstractFAPI2SPFinalServerTestModule {
}
