package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class EnsureDataArrayIsEmptyTest extends AbstractJsonResponseConditionUnitTest {


	@UseResurce("jsonResponses/resourcesAPI/v2/resourcesAPIResponseEmptyData.json")
	@Test
	public void happyPath() {
		EnsureDataArrayIsEmpty cond = new EnsureDataArrayIsEmpty();
		run(cond);
	}

	@UseResurce("jsonResponses/resourcesAPI/v2/resourcesAPIResponse.json")
	@Test
	public void unhappyPath() {
		EnsureDataArrayIsEmpty cond = new EnsureDataArrayIsEmpty();

		ConditionError conditionError = runAndFail(cond);
		assertThat(conditionError.getMessage(), containsString("Data array should be empty, but it was not."));
	}
}
