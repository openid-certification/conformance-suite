package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.AddAuthorizationSigningAlgValuesSupportedToServerConfiguration;
import net.openid.conformance.condition.as.AddJARMResponseModeToServerConfiguration;
import net.openid.conformance.condition.as.AddResponseTypeCodeToServerConfiguration;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class AddJARMToServerConfiguration extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(AddResponseTypeCodeToServerConfiguration.class, "FAPI1-ADV-5.2.2-2");
		callAndStopOnFailure(AddJARMResponseModeToServerConfiguration.class, "FAPI1-ADV-5.2.2.2");
		callAndStopOnFailure(AddAuthorizationSigningAlgValuesSupportedToServerConfiguration.class, "JARM-4", "FAPI1-ADV-8.6");
		//TODO add authorization_encryption_alg_values_supported and authorization_encryption_enc_values_supported? didn't seem necessary to me
	}
}
