package net.openid.conformance.apis.channels;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.channels.PhoneChannelsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class PhoneChannelsValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/channels/phoneChannels/createPhoneChannels_returnOk.json")
	public void validateStructureOK() {
		PhoneChannelsValidator condition = new PhoneChannelsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/channels/phoneChannels/createPhoneChannels_WithError.json")
	public void validateStructureWithMissingField() {
		PhoneChannelsValidator condition = new PhoneChannelsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("type")));
	}

	@Test
	@UseResurce("jsonResponses/channels/phoneChannels/createPhoneChannels_WrongEnum.json")
	public void validateStructureWithWrongEnum() {
		PhoneChannelsValidator condition = new PhoneChannelsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("type")));
	}
}
