package net.openid.conformance.vci10issuer.condition;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Resolves the notification endpoint URL from the credential issuer metadata.
 *
 * Per OID4VCI Section 12.2.4, the notification_endpoint is an optional metadata field
 * that contains the URL of the notification endpoint.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-12.2.4">OID4VCI Section 12.2.4 - Credential Issuer Metadata</a>
 */
public class VCIResolveNotificationEndpointToUse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		String notificationEndpointUrl = env.getString("vci", "credential_issuer_metadata.notification_endpoint");

		if (Strings.isNullOrEmpty(notificationEndpointUrl)) {
			throw error("notification_endpoint not found in credential issuer metadata.",
				args("credential_issuer_metadata", env.getElementFromObject("vci", "credential_issuer_metadata")));
		}

		log("Use notification endpoint from credential issuer metadata",
			args("notification_endpoint", notificationEndpointUrl));
		env.putString("resource", "resourceUrl", notificationEndpointUrl);

		return env;
	}
}
