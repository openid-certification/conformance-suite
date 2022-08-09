package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class ValidateNumberOfRecordsTest extends AbstractJsonResponseConditionUnitTest {
	private String metaOnlyRequestDateTime = "metaOnlyRequestDateTime";

	@Test
	@UseResurce("jsonResponses/account/transactions/accountTransactionsMaxPageSizeMetaGood1.json")
	public void testHappyPathMetaStandardPage1() {
		ValidateNumberOfRecordsPage1 cond = new ValidateNumberOfRecordsPage1();
		environment.putString(metaOnlyRequestDateTime,"false");
		run(cond);
	}

	@Test
	@UseResurce("jsonResponses/account/transactions/accountTransactionsMaxPageSizeMetaBadPageSize1.json")
	public void testUnhappyPathMetaStandardPage1WrongPageSize() {
		ValidateNumberOfRecordsPage1 cond = new ValidateNumberOfRecordsPage1();
		environment.putString(metaOnlyRequestDateTime,"false");

		ConditionError conditionError = runAndFail(cond);
		assertThat(conditionError.getMessage(), containsString("Number of records returned is different from specified in page-size"));
	}

	@Test
	@UseResurce("jsonResponses/account/transactions/accountTransactionsMaxPageSizeMetaGood2.json")
	public void testUnhappyPathMetaStandardPage1WrongPage() {
		ValidateNumberOfRecordsPage1 cond = new ValidateNumberOfRecordsPage1();
		environment.putString(metaOnlyRequestDateTime,"false");

		ConditionError conditionError = runAndFail(cond);
		assertThat(conditionError.getMessage(), containsString("Page number in the self link is incorrect"));
	}

	@Test
	@UseResurce("jsonResponses/account/transactions/accountTransactionsMaxPageSizeMetaOnlyGood1.json")
	public void testHappyPathMetaOnlyPage1() {
		ValidateNumberOfRecordsPage1 cond = new ValidateNumberOfRecordsPage1();
		environment.putString(metaOnlyRequestDateTime,"true");
		run(cond);
	}

	@Test
	@UseResurce("jsonResponses/account/transactions/accountTransactionsMaxPageSizeMetaOnlyGood1.json")
	public void testUnhappyPathNoMetaOnlyFieldInEnv() {
		ValidateNumberOfRecordsPage1 cond = new ValidateNumberOfRecordsPage1();
		ConditionError conditionError = runAndFail(cond);
		assertThat(conditionError.getMessage(), containsString("[pre] Something unexpected happened"));
	}

	@Test
	@UseResurce("jsonResponses/account/transactions/accountTransactionsMaxPageSizeMetaOnlyBadPageSize1.json")
	public void testUnhappyPathMetaOnlyPage1WrongPageSize() {
		ValidateNumberOfRecordsPage1 cond = new ValidateNumberOfRecordsPage1();
		environment.putString(metaOnlyRequestDateTime,"true");
		ConditionError conditionError = runAndFail(cond);
		assertThat(conditionError.getMessage(), containsString("Number of records returned is different from specified in page-size"));
	}

	@Test
	@UseResurce("jsonResponses/account/transactions/accountTransactionsMaxPageSizeMetaGood2.json")
	public void testHappyPathMetaStandardPage2() {
		ValidateNumberOfRecordsPage2 cond = new ValidateNumberOfRecordsPage2();

		environment.putString("number_of_returned_records_from_page_1", String.valueOf(3));
		environment.putString(metaOnlyRequestDateTime,"false");

		run(cond);
	}

	@Test
	@UseResurce("jsonResponses/account/transactions/accountTransactionsMaxPageSizeMetaOnlyGood2.json")
	public void testHappyPathMetaOnlyPage2() {
		ValidateNumberOfRecordsPage2 cond = new ValidateNumberOfRecordsPage2();

		environment.putString("number_of_returned_records_from_page_1", String.valueOf(3));
		environment.putString(metaOnlyRequestDateTime,"true");

		run(cond);
	}

	@Test
	@UseResurce("jsonResponses/account/transactions/accountTransactionsMaxPageSizeMetaOnlyBadPageSize2.json")
	public void testUnhappyPathMetaOnlyPage2WrongPage() {
		ValidateNumberOfRecordsPage1 cond = new ValidateNumberOfRecordsPage1();
		environment.putString(metaOnlyRequestDateTime,"true");

		ConditionError conditionError = runAndFail(cond);
		assertThat(conditionError.getMessage(), containsString("Page number in the self link is incorrect"));
	}
}

