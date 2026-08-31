package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.testmodule.Environment;

public class AddCdrArrangementIdClaimToAuthorizationEndpointRequest extends AbstractAddCdrArrangementIdClaimToAuthorizationEndpointRequest {

	@Override
	protected String arrangementId(Environment env) {
		String cdrArrangementId = env.getString("cdr_arrangement_id");
		if (Strings.isNullOrEmpty(cdrArrangementId)) {
			throw error("No cdr_arrangement_id found in the environment; it must be extracted from an earlier token endpoint response");
		}
		return cdrArrangementId;
	}

}
