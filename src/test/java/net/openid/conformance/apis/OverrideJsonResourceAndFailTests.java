package net.openid.conformance.apis;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.account.AccountListValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.UseResurce;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.fail;

public class OverrideJsonResourceAndFailTests extends AbstractJsonResponseConditionUnitTest {

	@UseResurce("jsonResponses/account/accountListResponse.json")
	@Test(expected = AssertionError.class)
	public void canOverrideResource() throws URISyntaxException {

		throw new RuntimeException("This test should not get as far as executing");

	}

	@BeforeClass
	public static void setupProperties() throws URISyntaxException {
		System.setProperty("resource.override", "/doesntexist.json");
	}

	@AfterClass
	public static void clearProperties() {
		System.clearProperty("resource.override");
	}

}
