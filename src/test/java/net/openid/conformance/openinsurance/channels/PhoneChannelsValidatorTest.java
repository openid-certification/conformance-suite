package net.openid.conformance.openinsurance.channels;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.channels.PhoneChannelsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

@UseResurce("openinsuranceResponses/channels/PhoneChannelsResponse.json")
public class PhoneChannelsValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new PhoneChannelsValidator());
	}
}
