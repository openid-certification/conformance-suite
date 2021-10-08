package net.openid.conformance.apis.channels;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.channels.BranchesChannelsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class BranchChannelValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/channels/branchesChannels/createBranchesChannels_returnOk.json")
	public void validateStructureOK() {
		BranchesChannelsValidator condition = new BranchesChannelsValidator();
		run(condition);
	}


	@Test
	@UseResurce("jsonResponses/channels/branchesChannels/createBranchesChannels_WithError.json")
	public void validateStructureWithMissingField() {
		BranchesChannelsValidator condition = new BranchesChannelsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("cnpjNumber")));
	}

	@Test
	@UseResurce("jsonResponses/channels/branchesChannels/createBranchesChannels_WrongEnum.json")
	public void validateStructureWithWrongEnum() {
		BranchesChannelsValidator condition = new BranchesChannelsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("weekday")));
	}

	@Test
	@UseResurce("jsonResponses/channels/branchesChannels/createBranchesChannels_WrongMaxLength.json")
	public void validateStructureWithWrongMaxLength() {
		BranchesChannelsValidator condition = new BranchesChannelsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("countryCallingCode")));
	}
}
