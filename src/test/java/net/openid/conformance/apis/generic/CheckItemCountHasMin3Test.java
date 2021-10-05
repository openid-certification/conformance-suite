package net.openid.conformance.apis.generic;

import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.client.CheckItemCountHasMin3;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/metaData/goodResponseWith3Dataitems.json")
public class CheckItemCountHasMin3Test extends AbstractJsonResponseConditionUnitTest {
    
    @Test
	public void validateMetaDataAndLinks() {
		CheckItemCountHasMin3 condition = new CheckItemCountHasMin3();
		run(condition);
	}

    @Test
	@UseResurce("jsonResponses/metaData/badItemCountMetaResponse.json")
	public void validateStructureWithoutData() {
		CheckItemCountHasMin3 condition = new CheckItemCountHasMin3();
		ConditionError error = runAndFail(condition);
		String expected = "The response does not contain the required minimum number of data elements.";
		assertThat(error.getMessage(), containsString(expected));
	}
}

    
