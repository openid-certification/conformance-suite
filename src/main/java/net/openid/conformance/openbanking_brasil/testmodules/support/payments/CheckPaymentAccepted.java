package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.common.collect.Sets;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Objects;
import java.util.Set;

public class CheckPaymentAccepted extends AbstractCondition {

	private final Set<String> STATUS = Sets.newHashSet("ACSP", "ACSC", "ACCC");

	@Override
	@PreEnvironment(strings = "payment_status")
	public Environment evaluate(Environment env) {
		String status = env.getString("payment_status");
		if (STATUS.contains(status)) {
			logSuccess("Payment status is accepted: " + status);
		} else if (Objects.equals(status, "RJCT")) {
			logFailure("Payment status is rejects");
		} else {
			logFailure("Invalid payment status: " + status);
		}

		return env;
	}
}
