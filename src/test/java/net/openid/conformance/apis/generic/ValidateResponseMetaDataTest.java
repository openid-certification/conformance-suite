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
		String expected = "totalPages field should not be 0 or 1.";
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
		String expected = "Invalid 'self' link URI.";
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

	@Test
	@UseResurce("jsonResponses/metaData/goodResponseWithoutMetadata.json")
	public void validateStructureWithoutMetatdata() {
		ValidateResponseMetaData condition = new ValidateResponseMetaData();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/metaData/goodResponseWithSelfLinkOnly.json")
	public void validateStructureWithSelfLinkOnly() {
		ValidateResponseMetaData condition = new ValidateResponseMetaData();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/metaData/goodConsentResponseWithoutSelfOrMeta.json")
	public void validateStructureWithoutSelfOrMeta() {
		ValidateResponseMetaData condition = new ValidateResponseMetaData();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/metaData/badResponseWithoutSelfLink.json")
	public void validateStructureWithoutSelfLink() {
		ValidateResponseMetaData condition = new ValidateResponseMetaData();
		ConditionError error = runAndFail(condition);
		String expected = "There should be a 'self' link.";
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/metaData/badResponseWithMissingTimeZone.json")
	public void validateStructureWithMissingTimeZone() {
		ValidateResponseMetaData condition = new ValidateResponseMetaData();
		ConditionError error = runAndFail(condition);
		String expected = "requestDateTime is not in valid RFC 3339 format.";
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/metaData/badResponseWithDateTimeOffset.json")
	public void validateStructureWithDateTimeOffset() {
		ValidateResponseMetaData condition = new ValidateResponseMetaData();
		ConditionError error = runAndFail(condition);
		String expected = "requestDateTime is more than 20 characters in length.";
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/metaData/badPaymentConsentResponseWithoutSelf.json")
	public void validatePaymentConsentStructureWithoutLinks() {
		ValidateResponseMetaData condition = new ValidateResponseMetaData();
		ConditionError error = runAndFail(condition);
		String expected = "Payment consent requires a 'self' link.";
		assertThat(error.getMessage(), containsString(expected));
	}
	@Test
	@UseResurce("jsonResponses/metaData/goodResponseWithEmptyData.json")
	public void validateResponseWithEmptyData() {
		ValidateResponseMetaData condition = new ValidateResponseMetaData();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/metaData/badResponseWithEmptyData.json")
	public void validateBadResponseWithEmptyData() {
		ValidateResponseMetaData condition = new ValidateResponseMetaData();
		ConditionError error = runAndFail(condition);
		String expected = "totalPages and totalRecords fields have to be 0 when data array is empty";
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/metaData/badResponseWith3DataitemsAnd0Meta.json")
	public void validateBadResponseWith3DataItemsAnd0Meta() {
		ValidateResponseMetaData condition = new ValidateResponseMetaData();
		ConditionError error = runAndFail(condition);
		String expected = "Data contains more items than the metadata totalRecords.";
		assertThat(error.getMessage(), containsString(expected));
	}

}
