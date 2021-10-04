package net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;

public class CustomerDataResources404 extends AbstractJsonAssertingCondition {

	@Override
	@PostEnvironment(strings = "warning_message")
	public Environment evaluate(Environment env) {
		log("Setting warning message");
		env.putString("warning_message", "To certifier: Check the permissions codes returned in the consent creation were for customer information permissions only and that the Bank only will support customer information. Review the Banks personal or business customer data submission to confirm that consents are being authorised correctly");
		return env;
	}
}
