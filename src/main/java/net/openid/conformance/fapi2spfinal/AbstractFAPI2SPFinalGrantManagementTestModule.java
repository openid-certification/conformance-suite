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
 * <p>The static {@code @VariantNotApplicable} on {@link FAPI2FinalOPProfile} states that restriction
 * directly. The conditional {@code @VariantNotApplicableWhen(GrantManagement=enabled, ...)} on
 * {@link AbstractFAPI2SPFinalServerTestModule} hides the value in the UI dropdown for those profiles.
 */
@VariantNotApplicable(parameter = GrantManagement.class, values = {"disabled"})
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = {"consumerdataright_au", "openbanking_brazil", "connectid_au", "cbuae", "ksa",
	"fapi_client_credentials_grant", "vci", "vci_haip"})
public abstract class AbstractFAPI2SPFinalGrantManagementTestModule extends AbstractFAPI2SPFinalServerTestModule {

	/** Every module in this family calls the grant management endpoint, so all of them need the GM 6.1 scopes. */
	@Override
	protected boolean needsGrantManagementApiScopes() {
		return true;
	}
}
