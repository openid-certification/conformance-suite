package net.openid.conformance.apis.channels;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.channels.SharedAutomatedTellerMachinesValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class SharedAutomatedTellerMachinesChannelValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/channels/sharedAutomatedTellerMachinesChannels/createSharedAutomatedTellerMachinesChannels_returnOK.json")
	public void validateStructureOK() {
		SharedAutomatedTellerMachinesValidator condition = new SharedAutomatedTellerMachinesValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/channels/sharedAutomatedTellerMachinesChannels/createSharedAutomatedTellerMachinesChannels_WithError.json")
	public void validateStructureWithMissingField() {
		SharedAutomatedTellerMachinesValidator condition = new SharedAutomatedTellerMachinesValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("cnpjNumber", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/channels/sharedAutomatedTellerMachinesChannels/createSharedAutomatedTellerMachinesChannels_WrongEnum.json")
	public void validateStructureWithWrongEnum() {
		SharedAutomatedTellerMachinesValidator condition = new SharedAutomatedTellerMachinesValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("weekday", condition.getApiName())));
	}

}
