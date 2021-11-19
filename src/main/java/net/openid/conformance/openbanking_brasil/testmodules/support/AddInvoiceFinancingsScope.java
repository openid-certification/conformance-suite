package net.openid.conformance.openbanking_brasil.testmodules.support;

public class AddInvoiceFinancingsScope extends AbstractScopeAddingCondition {
	@Override
	protected String newScope() {
		return "invoice-financings";
	}
}
