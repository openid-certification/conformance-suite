package net.openid.conformance.apis.generic;

import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openbanking_brasil.testmodules.support.ValidateResponseMetaData;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/metaData/goodMetaLinksBodyResponse.json")
public class ValidateResponseMetaDataTest extends AbstractJsonResponseConditionUnitTest {
    
    @Test
	public void validateMetaDataAndLinks() {
		ValidateResponseMetaData condition = new ValidateResponseMetaData();
		run(condition);
	}

    @Test
	@UseResurce("jsonResponses/metaData/badMetaLinksBodyResponse.json")
	public void validateStructureWithMissingLinksObject() {
		ValidateResponseMetaData condition = new ValidateResponseMetaData();
		ConditionError error = runAndFail(condition);
		String expected = "There should not be a 'next' or 'prev' link.";
		assertThat(error.getMessage(), containsString(expected));
	}

    @Test
	@UseResurce("jsonResponses/metaData/badItemCountMetaResponse.json")
	public void validateStructureWithIncorrectItemCount() {
		ValidateResponseMetaData condition = new ValidateResponseMetaData();
		ConditionError error = runAndFail(condition);
		String expected = "Data contains more items than the metadata totalRecords.";
		assertThat(error.getMessage(), containsString(expected));
	}

    @Test
	@UseResurce("jsonResponses/metaData/badSelfLinkResponse.json")
	public void validateStructureWithInvalidSelfLink() {
		ValidateResponseMetaData condition = new ValidateResponseMetaData();
		ConditionError error = runAndFail(condition);
		String expected = "Invalid Self Link URI.";
		assertThat(error.getMessage(), containsString(expected));
	}

    @Test
	@UseResurce("jsonResponses/metaData/badResponseWithPrevLink.json")
	public void validateStructureWithPrevLink() {
		ValidateResponseMetaData condition = new ValidateResponseMetaData();
		ConditionError error = runAndFail(condition);
		String expected = "There should not be a 'prev' link.";
		assertThat(error.getMessage(), containsString(expected));
	}

    @Test
	@UseResurce("jsonResponses/metaData/badResponseWithMissingPrevLink.json")
	public void validateStructureWithMissingPrevLink() {
		ValidateResponseMetaData condition = new ValidateResponseMetaData();
		ConditionError error = runAndFail(condition);
		String expected = "There should be a 'prev' link.";
		assertThat(error.getMessage(), containsString(expected));
	}

    @Test
	@UseResurce("jsonResponses/metaData/badResponseWithNextLink.json")
	public void validateStructureWithNextLink() {
		ValidateResponseMetaData condition = new ValidateResponseMetaData();
		ConditionError error = runAndFail(condition);
		String expected = "There should not be a 'next' link.";
		assertThat(error.getMessage(), containsString(expected));
	}

    @Test
	@UseResurce("jsonResponses/metaData/badResponseWithMissingNextLink.json")
	public void validateStructureWithMissingNextLink() {
		ValidateResponseMetaData condition = new ValidateResponseMetaData();
		ConditionError error = runAndFail(condition);
		String expected = "There should be a 'next' link.";
		assertThat(error.getMessage(), containsString(expected));
	}
}
