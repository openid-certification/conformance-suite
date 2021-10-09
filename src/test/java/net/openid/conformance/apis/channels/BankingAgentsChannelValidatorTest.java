package net.openid.conformance.apis.channels;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.channels.BankingAgentsChannelValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class BankingAgentsChannelValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/channels/bankingAgentsChannels/createBankingAgentsChannels_returnOk.json")
	public void validateStructureOK() {
		BankingAgentsChannelValidator condition = new BankingAgentsChannelValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/channels/bankingAgentsChannels/createBankingAgentsChannels_WithError.json")
	public void validateStructureWithMissingField() {
		BankingAgentsChannelValidator condition = new BankingAgentsChannelValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("cnpjNumber")));
	}

	@Test
	@UseResurce("jsonResponses/channels/bankingAgentsChannels/createBankingAgentsChannels_WrongEnum.json")
	public void validateStructureWithWrongEnum() {
		BankingAgentsChannelValidator condition = new BankingAgentsChannelValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("weekday")));
	}

	@Test
	@UseResurce("jsonResponses/channels/bankingAgentsChannels/createBankingAgentsChannels_WrongMaxLength.json")
	public void validateStructureWithWrongMaxLength() {
		BankingAgentsChannelValidator condition = new BankingAgentsChannelValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsMoreThanMaxLengthMessage("additionalInfo")));
	}
}
