package net.openid.conformance.logging;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonObjectBuilder;

/**
 * Special case class for catching the initial config rendering by TestRunner
 */
public class TestConfigLeafNodeVisitor implements JsonLeafNodeVisitor, MapLeafNodeVisitor {

	@Override
	public void accept(JsonObjectSanitiser.LeafNode leafNode) {

	}

	@Override
	public void accept(MapSanitiser.LeafNode leafNode) {
		if(!leafNode.getSource().equals("TEST-RUNNER")) {
			return;
		}
		if(!leafNode.getKey().equals("config")) {
			return;
		}
		JsonObject config = (JsonObject) leafNode.getProperty();
		JsonObject newConfig = new JsonObjectBuilder()
			.addField("server.discoveryUrl", OIDFJSON.getString(config.getAsJsonObject("server").get("discoveryUrl")))
			.addField("restOfConfig", "obscured for security reasons")
			.build();
		leafNode.replace(newConfig);
	}
}
