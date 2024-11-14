package net.openid.conformance.openid.federation;

import org.springframework.http.MediaType;

import java.util.Collections;
import java.util.List;

public class CallEntityStatementEndpointAndReturnFullResponse extends AbstractCallFederationEndpointAndReturnFullResponse {

	@Override
	protected List<MediaType> getAcceptHeader() {
		return Collections.singletonList(new MediaType("application", "entity-statement+jwt"));
	}

}
