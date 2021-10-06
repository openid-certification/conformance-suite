package net.openid.conformance.apis.generic;

import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.client.CheckItemCountHasMin1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/metaData/goodMetaLinksBodyResponse.json")
public class CheckItemCountHasMin1Test extends AbstractJsonResponseConditionUnitTest {
    
    @Test
	public void validateMetaDataAndLinks() {
		CheckItemCountHasMin1 condition = new CheckItemCountHasMin1();
		run(condition);
	}

    @Test
	@UseResurce("jsonResponses/metaData/badResponseWithoutSelfLink.json")
	public void validateStructureWithoutSelfLink() {
		CheckItemCountHasMin1 condition = new CheckItemCountHasMin1();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/metaData/badResponseWithoutData.json")
	public void validateStructureWithoutData() {
		CheckItemCountHasMin1 condition = new CheckItemCountHasMin1();
		ConditionError error = runAndFail(condition);
		String expected = "The response does not contain the required minimum number of data elements.";
		assertThat(error.getMessage(), containsString(expected));
	}
}

    
