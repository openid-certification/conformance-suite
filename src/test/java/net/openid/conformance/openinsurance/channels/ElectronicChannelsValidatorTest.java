package net.openid.conformance.openinsurance.channels;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.channels.v1.ElectronicChannelsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

@UseResurce("openinsuranceResponses/channels/ElectronicChannelsResponse.json")
public class ElectronicChannelsValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new ElectronicChannelsValidator());
	}
}
