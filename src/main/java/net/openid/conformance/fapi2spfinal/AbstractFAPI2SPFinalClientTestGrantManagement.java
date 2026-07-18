package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.GrantManagement;
import net.openid.conformance.variant.VariantNotApplicable;

/**
 * Shared base for FAPI2-SP-Final client (RP) grant management test modules.
 *
 * <p>Grant management only makes sense for profiles that perform an authorization flow that yields
 * a grant, so these modules must not be generated for the client credentials grant profile (which
 * has no such flow). The test plans exclude that profile at the plan level, but that guard only
 * fires when a plan is instantiated through the UI/plan API; a module invoked directly with a config
 * (as the integration tests do) is only protected by the module's own {@code @VariantNotApplicable}.
 * This mirrors {@link AbstractFAPI2SPFinalGrantManagementTestModule} on the server (OP) side.
 *
 * <p>Unlike the server base, the VCI profiles are <em>not</em> excluded here: these client (RP)
 * modules are deliberately reused under {@code vci_haip} by {@code VCIWalletTestPlanHaip} to
 * exercise grant management in the VCI wallet flow.
 */
@VariantNotApplicable(parameter = GrantManagement.class, values = {"disabled"})
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = {"fapi_client_credentials_grant"})
public abstract class AbstractFAPI2SPFinalClientTestGrantManagement extends AbstractFAPI2SPFinalClientTest {
}
