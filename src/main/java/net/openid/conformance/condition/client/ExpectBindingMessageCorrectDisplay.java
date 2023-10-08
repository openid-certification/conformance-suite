package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectBindingMessageCorrectDisplay extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		String automatedApprovalUrl = env.getString("config", "automated_ciba_approval_url");

		if (!Strings.isNullOrEmpty(automatedApprovalUrl)) {

			throw error("Automated approval url has been provided in the configuration json. It is assumed this is an automated run and the display of the binding message cannot be verified.");

		}

		String unusedPlaceholder = createBrowserInteractionPlaceholder("If the server does not return the invalid_binding_message error. It must authenticate successfully and the binding message being correctly displayed - upload a screenshot/photo of the binding message");

		return env;
	}
}
