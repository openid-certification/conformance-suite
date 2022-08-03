package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class ValidateNumberOfRecordsPage2 extends ValidateNumberOfRecords {
	@Override
	@PreEnvironment(strings = {"number_of_returned_records_from_page_1", "metaOnlyRequestDateTime"})
	public Environment evaluate(Environment env) {
		prepareRecordData(env);
		int numberOfRecordsFromFirstPage = Integer.parseInt(env.getString("number_of_returned_records_from_page_1"));

		if (currentPageNumber != 2) {
			throw error("Page number in the self link is incorrect",
				Map.of("Self link", selfLink,
					"Provided page number", currentPageNumber,
					"Expected page number", 2));

		} else {
			logSuccess("Page number matches the number in the self link");
		}

		validatePrevLink();

		if (totalNumberOfPages > currentPageNumber) {
			log("Second page is not the last page");

			if (numberOfReturnedRecords != pageSize) {
				throw error("Number of records returned has to be equal to the page-size",
					Map.of("Number of returned records", numberOfReturnedRecords,
						"Page size", pageSize));
			} else {
				logSuccess("Number of records returned is equal to the page-size");
			}

			if (totalNumberOfRecords <= numberOfRecordsFromFirstPage + numberOfReturnedRecords) {
				throw error("Total number of records has to be greater than number of records in page 1 and 2",
					Map.of("Total number of records", totalNumberOfRecords,
						"Number of records in page 1 and 2", numberOfRecordsFromFirstPage + numberOfReturnedRecords));
			} else {
				logSuccess("Total number of records is greater than number of records in page 1 and 2");
			}

			validateLastLink();
			validateNextLink();

		} else {
			log("Second page is the last page");

			if (numberOfReturnedRecords > pageSize) {
				throw error("Number of records returned has to be less than or equal to the page-size",
					Map.of("Number of returned records", numberOfReturnedRecords,
						"Page size", pageSize));
			} else {
				logSuccess("Number of records returned is less than or equal to the page-size");
			}

			if (!isMetaOnlyRequestDateTime) {
				if (totalNumberOfRecords != numberOfRecordsFromFirstPage + numberOfReturnedRecords) {
					throw error("Total number of records has to be equal to the number of records in page 1 and 2",
						Map.of("Total number of records", totalNumberOfRecords,
							"Number of records in page 1 and 2", numberOfRecordsFromFirstPage + numberOfReturnedRecords));
				} else {
					logSuccess("Total number of records is equal to the number of records in page 1 and 2");
				}
			}
		}

		return env;
	}

}

