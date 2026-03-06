package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = {"vci"})
public abstract class AbstractFAPI2SPFinalExpectingAuthorizationEndpointPlaceholderOrCallback extends AbstractFAPI2SPFinalServerTestModule {

	@Override
	protected void performRedirect() {
		performRedirectAndWaitForPlaceholdersOrCallback();
	}

	protected void performNormalRedirect() {
		// some subclasses need access to the original performRedirect, this just makes it available to them
		super.performRedirect();
	}

}
