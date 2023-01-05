package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.as.FAPIBrazilEncryptRequestObject;
import net.openid.conformance.condition.client.AddAudToRequestObject;
import net.openid.conformance.condition.client.AddClientIdToRequestObject;
import net.openid.conformance.condition.client.AddExpToRequestObject;
import net.openid.conformance.condition.client.AddIatToRequestObject;
import net.openid.conformance.condition.client.AddIssToRequestObject;
import net.openid.conformance.condition.client.AddNbfToRequestObject;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.SignKSARequestObject;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class CreateKSAAuthorizationRequestObjectSteps extends AbstractConditionSequence {

	protected boolean isSecondClient;
	protected boolean encrypt;

	public CreateKSAAuthorizationRequestObjectSteps(boolean isSecondClient, boolean encrypt) {
		this.isSecondClient = isSecondClient;
		this.encrypt = encrypt;
	}

	@Override
	public void evaluate() {
		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

		if (isSecondClient) {
			callAndStopOnFailure(AddIatToRequestObject.class);
		}
		callAndStopOnFailure(AddNbfToRequestObject.class, "FAPI1-ADV-5.2.2-17"); // mandatory in FAPI1-Advanced-Final
		callAndStopOnFailure(AddExpToRequestObject.class, "FAPI1-ADV-5.2.2-13");

		callAndStopOnFailure(AddAudToRequestObject.class, "FAPI1-ADV-5.2.2-14");

		// iss is a 'should' in OIDC & jwsreq,
		callAndStopOnFailure(AddIssToRequestObject.class, "OIDCC-6.1");

		// jwsreq-26 is very explicit that client_id should be both inside and outside the request object
		callAndStopOnFailure(AddClientIdToRequestObject.class, "FAPI1-ADV-5.2.3-8");

		callAndStopOnFailure(SignKSARequestObject.class);

		if (encrypt) {
			callAndStopOnFailure(FAPIBrazilEncryptRequestObject.class, "BrazilOB-5.2.2-1", "BrazilOB-6.1.1-1");
		}
	}
}
