package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.GrantManagement;
import net.openid.conformance.variant.VariantNotApplicable;

/**
 * Shared base for FAPI2-SP-Final OP grant management test modules.
 *
 * <p>Grant management only makes sense for profiles that perform an authorization flow that yields
 * a grant, so these modules must not be generated for the client credentials grant or VCI profiles.
 *
 * <p>The static {@code @VariantNotApplicable} on {@link FAPI2FinalOPProfile} is what actually drops
 * these modules from those profiles. The conditional
 * {@code @VariantNotApplicableWhen(GrantManagement=enabled, profile in {fapi_client_credentials_grant,
 * vci, vci_haip})} on {@link AbstractFAPI2SPFinalServerTestModule} is not sufficient on its own:
 * because these modules also mark {@code GrantManagement=disabled} not-applicable, the effective set
 * of allowed {@code GrantManagement} values collapses to empty under those profiles, and
 * {@code VariantService.isApplicableForVariant} treats an empty effective set as "parameter not
 * applicable, skip" rather than dropping the module. Excluding the profile values directly gives a
 * non-empty effective set that simply does not contain the selected profile, so the module is
 * correctly dropped.
 */
@VariantNotApplicable(parameter = GrantManagement.class, values = {"disabled"})
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = {"fapi_client_credentials_grant", "vci", "vci_haip"})
public abstract class AbstractFAPI2SPFinalGrantManagementTestModule extends AbstractFAPI2SPFinalServerTestModule {
}
