package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class ValidatePageSizeIsOne extends ValidateNumberOfRecords {
	@Override
	public Environment evaluate(Environment environment) {
		prepareRecordData(environment);

		if (currentPageNumber != 1) {
			throw error("Page number in the self link is incorrect",
				Map.of("Self link", selfLink,
					"Provided page number", currentPageNumber,
					"Expected page number", 1));

		} else {
			logSuccess("Page number matches the number in the self link");
		}

		validateNextLink();

		return environment;
	}
}
