package net.openid.conformance.raidiam.validators.organisations;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: ****
 * Api endpoint: GET /organisations/{OrganisationId}/ess/poll/{ExternalSigningServiceEnvelopeId}
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory Get Organisations ByEnvelopeId")
public class GetOrganisationsEssPollByEnvelopeIdValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> ENVELOPE_STATUS = Sets.newHashSet("completed", "created", "declined",
		"deleted", "delivered", "processing", "sent", "signed", "template", "voided", "expired");

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement content = bodyFrom(environment);

				assertField(content,
					new StringField
						.Builder("OrganisationId")
						.setMinLength(1)
						.setMaxLength(40)
						.build());

				assertField(content,
					new StringField
						.Builder("EssEnvelopeId")
						.build());

				assertField(content,
					new StringField
						.Builder("ExternalSigningServiceEnvelopeStatus")
						.setEnums(ENVELOPE_STATUS)
						.setOptional()
						.build());

		return environment;
	}
}
