package net.openid.conformance.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnforcePresenceOfDebtorAccount;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class EnforcePresenceOfDebtorAccountTest {

	@Test
	public void debtorAccountPresentIsFine() throws IOException {

		String rawJson = IOUtils.resourceToString("test_config.json", Charset.defaultCharset(), getClass().getClassLoader());

		JsonObject config = new JsonParser().parse(rawJson).getAsJsonObject();

		Environment environment = new Environment();
		environment.putObject("config", config);

		EnforcePresenceOfDebtorAccount condition = new EnforcePresenceOfDebtorAccount();
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
		condition.evaluate(environment);

	}

	@Test
	public void debtorAccountMissingErrors() throws IOException {

		String rawJson = IOUtils.resourceToString("test_config_no_debtor.json", Charset.defaultCharset(), getClass().getClassLoader());

		JsonObject config = new JsonParser().parse(rawJson).getAsJsonObject();

		Environment environment = new Environment();
		environment.putObject("config", config);

		EnforcePresenceOfDebtorAccount condition = new EnforcePresenceOfDebtorAccount();
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
		try {
			condition.evaluate(environment);
			fail("An exception should have been thrown");
		} catch(ConditionError e) {}

	}

}
