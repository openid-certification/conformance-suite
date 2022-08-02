package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.util.JsonUtils;
import net.openid.conformance.util.UseResurce;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AbstractCheckExpectedDateResponseTest extends AbstractJsonResponseConditionUnitTest {

	private static final Gson GSON = JsonUtils.createBigDecimalAwareGson();

	@Test
	@UseResurce("jsonResponses/creditCard/creditCardV2/cardTransactionsCurrentV2/cardTransactionsCurrentResponse.json")
	public void test(){
		addFullRangeResponseToEnvironment("jsonResponses/creditCard/creditCardV2/cardTransactionsCurrentV2/cardTransactionsCurrentFullRangeResponse.json");
		CheckExpectedTransactionDateMaxLimitedResponse condition = new CheckExpectedTransactionDateMaxLimitedResponse();
		run(condition);
	}



	private void addFullRangeResponseToEnvironment(String path){
		try {
			String json = IOUtils.resourceToString(path, StandardCharsets.UTF_8, getClass().getClassLoader());
			JsonObject fullRangeResponse = GSON.fromJson(json, JsonObject.class);
			environment.putObject("full_range_response", fullRangeResponse);
		} catch (IOException e) {
			throw new AssertionError("Could not load resource");
		}
	}


}
