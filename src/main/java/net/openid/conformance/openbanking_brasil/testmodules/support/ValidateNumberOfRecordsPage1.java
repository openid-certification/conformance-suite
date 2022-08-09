package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class ValidateNumberOfRecordsPage1 extends ValidateNumberOfRecords {
	@Override
	@PreEnvironment(strings = "metaOnlyRequestDateTime")
	@PostEnvironment(strings = "number_of_returned_records_from_page_1")
	public Environment evaluate(Environment env) {
		prepareRecordData(env);
		env.putString("number_of_returned_records_from_page_1", String.valueOf(numberOfReturnedRecords));

		if (currentPageNumber != 1) {
			throw error("Page number in the self link is incorrect",
				Map.of("Self link", selfLink,
					"Provided page number", currentPageNumber,
					"Expected page number", 1));

		} else {
			logSuccess("Page number matches the number in the self link");
		}

		if (numberOfReturnedRecords != pageSize) {
			throw error("Number of records returned is different from specified in page-size",
				Map.of("Provided page-size", pageSize,
					"Number of returned records", numberOfReturnedRecords));
		} else {
			logSuccess("Number of records match accordingly");
		}

		if(!isMetaOnlyRequestDateTime) {
			if (totalNumberOfPages > currentPageNumber) {
				validateNextLink();
				validateLastLink();
			}
		}
		return env;
	}

}
