package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Random;

public class InsertRandom1333XXAmountIntoPaymentsAndPaymentsConsentsResources extends AbstractCondition {
	@Override
	@PreEnvironment(required = "resource")
	public Environment evaluate(Environment env) {
		Random random = new Random();
		int amountFractionalPart = random.ints(0, 99)
			.findFirst().orElseThrow(() -> error("Could not generate random fractional part"));

		// Amount has to be always within the following range 1333.00 - 1333.99
		String amount = String.format("1333.%02d", amountFractionalPart);
		env.putString("resource", "brazilPaymentConsent.data.payment.amount", amount);
		env.putString("resource", "brazilPixPayment.data.payment.amount", amount);
		logSuccess("Inserted random amount into the payments and payments consents resources", args("amount", amount));
		return env;
	}
}
