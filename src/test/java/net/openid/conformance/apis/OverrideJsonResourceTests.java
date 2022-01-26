package net.openid.conformance.apis;

import net.openid.conformance.openbanking_brasil.account.AccountBalancesResponseValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.UseResurce;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

public class OverrideJsonResourceTests extends AbstractJsonResponseConditionUnitTest {

	@UseResurce("jsonResponses/account/balances/accountBalancesResponse.json")
	@Test
	public void canOverrideResource() throws URISyntaxException {

		AccountBalancesResponseValidator condition = new AccountBalancesResponseValidator() {

			@Override
			public Environment evaluate(Environment environment) {
				environment = super.evaluate(environment);
				assertJsonField(bodyFrom(environment), "$.links.self", "Overridden JSON file");
				return environment;
			}
		};

		run(condition);

	}

	@BeforeClass
	public static void setupProperties() throws URISyntaxException {
		URL res = OverrideJsonResourceTests.class.getClassLoader().getResource("jsonResponses/overriddenAccountBalances.json");
		File file = Paths.get(res.toURI()).toFile();
		String absolutePath = file.getAbsolutePath();
		System.setProperty("resource.override", absolutePath);
	}

	@AfterClass
	public static void clearProperties() {
		System.clearProperty("resource.override");
	}

}
