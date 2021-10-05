package net.openid.conformance.apis.channels;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.channels.ElectronicChannelsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class ChannelValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/channels/electronicChannels/createElectronicChannels_returnOk.json")
	public void validateStructureOK() {
		ElectronicChannelsValidator condition = new ElectronicChannelsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/channels/electronicChannels/createElectronicChannels_WithError.json")
	public void validateStructureWithMissingField() {
		ElectronicChannelsValidator condition = new ElectronicChannelsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("cnpjNumber")));
	}

	@Test
	@UseResurce("jsonResponses/channels/electronicChannels/createElectronicChannels_WrongEnum.json")
	public void validateStructureWithWrongEnum() {
		ElectronicChannelsValidator condition = new ElectronicChannelsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("name")));
	}
}
