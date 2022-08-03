package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class EnsureTransactionsDateIsNoOlderThan7DaysTest extends AbstractJsonResponseConditionUnitTest {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	@UseResurce("jsonResponses/account/accountV2/transactionscurrentV2/accountsTransactionsCurrentGood.json")
	@Test
	public void happyPath() {
		JsonObject transaction = jsonObject.getAsJsonArray("data").get(0).getAsJsonObject();
		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
		transaction.addProperty("transactionDate", currentDate.format(FORMATTER));

		EnsureTransactionsDateIsNoOlderThan7Days cond = new EnsureTransactionsDateIsNoOlderThan7Days();
		environment.putObject("resource_endpoint_response_full", "body", jsonObject);
		run(cond);
	}

	@UseResurce("jsonResponses/account/accountV2/transactionscurrentV2/accountsTransactionsCurrentGood.json")
	@Test
	public void unhappyPath() {
		EnsureTransactionsDateIsNoOlderThan7Days cond = new EnsureTransactionsDateIsNoOlderThan7Days();
		environment.putObject("resource_endpoint_response_full", "body", jsonObject);

		ConditionError conditionError = runAndFail(cond);
		assertThat(conditionError.getMessage(), containsString("Transaction is older than 7 days"));
	}

	@UseResurce("jsonResponses/account/accountV2/transactionscurrentV2/accountsTransactionsCurrentEmptyDataGood.json")
	@Test
	public void unhappyPathEmptyData() {
		EnsureTransactionsDateIsNoOlderThan7Days cond = new EnsureTransactionsDateIsNoOlderThan7Days();
		environment.putObject("resource_endpoint_response_full", "body", jsonObject);
		run(cond);
	}
}
