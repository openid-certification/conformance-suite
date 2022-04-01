package net.openid.conformance.openbanking_brasil.testmodules.support;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum EnumResourcesType {
	ACCOUNT, CREDIT_CARD_ACCOUNT, LOAN, FINANCING, UNARRANGED_ACCOUNT_OVERDRAFT, INVOICE_FINANCING;

	public static Set<String> allTypes() {
		return Arrays.stream(values())
			.map(Enum::name)
			.collect(Collectors.toSet());
	}
}
