package net.openid.conformance.openbanking_brasil.testmodules.customerAPI;

import net.openid.conformance.openbanking_brasil.testmodules.support.AbstractScopeAddingCondition;

public class OpinAddScopesForAll extends AbstractScopeAddingCondition {

	@Override
	protected String newScope() {
		return "consents resources customers insurance-acceptance-and-branches-abroad insurance-auto " +
			"insurance-aviation insurance-financial-risk insurance-nautical insurance-nuclear insurance-patrimonial " +
			"insurance-petroleum insurance-responsibility";
	}

}
