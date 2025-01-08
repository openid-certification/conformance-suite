package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFUpdateStreamConfigCall extends OIDSSFCreateStreamConfigCall {

	@Override
	protected String getEndpointName() {
		return "update stream configuration";
	}

	@Override
	protected void prepareRequest(Environment env) {
		super.prepareRequest(env);
		env.putString("resource", "resourceMethod", "PATCH");
	}

	@Override
	protected JsonObject createResourceRequestEntity(Environment env) {
		JsonObject streamConfig = super.createResourceRequestEntity(env);
		String streamId = getStreamId(env);
		streamConfig.addProperty("stream_id", streamId);
		streamConfig.addProperty("description", "Updated Stream for Receiver OIDF Conformance Test-Suite");
		return streamConfig;
	}
}
