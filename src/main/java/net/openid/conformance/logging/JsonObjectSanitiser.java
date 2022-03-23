package net.openid.conformance.logging;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.HashSet;
import java.util.Set;

public class JsonObjectSanitiser {

	public static class LeafNode {

		private final String source;
		private JsonObject owner;
		private JsonElement property;
		private String key;
		private LeafType type;

		public LeafNode(String source, JsonObject owner, JsonElement property, String key, LeafType type) {
			this.source = source;
			this.owner = owner;
			this.property = property;
			this.key = key;
			this.type = type;
		}

		public LeafType getType() {
			return type;
		}

		public JsonElement getProperty() {
			return property;
		}

		public void replace(JsonElement replacement) {
			owner.add(key, replacement);
		}

		public void visit(JsonLeafNodeVisitor sanitiser) {
			sanitiser.accept(this);
		}

		public String getKey() {
			return key;
		}

		public String getSource() {
			return source;
		}
	}

	private final Set<JsonLeafNodeVisitor> sanitisers;

	public JsonObjectSanitiser(Set<JsonLeafNodeVisitor> sanitisers) {
		this.sanitisers = sanitisers;
	}

	public void sanitise(String source, JsonObject jsonObject) {
		Set<LeafNode> leafNodes = findLeafNodes(source, jsonObject);
		sanitise(leafNodes);
	}

	public void sanitise(Set<LeafNode> leafNodes) {
		sanitisers.stream()
			.forEach(s -> runSanitiser(s, leafNodes));
	}

	private void runSanitiser(JsonLeafNodeVisitor sanitiser, Set<LeafNode> leafNodes) {
		leafNodes.stream()
			.forEach(n -> n.visit(sanitiser));
	}

	public Set<LeafNode> findLeafNodes(String source, JsonObject jsonObject) {
		Set<LeafNode> leafNodes = new HashSet<>();
		findLeafNodes(source, leafNodes, jsonObject);
		return leafNodes;
	}

	private void findLeafNodes(String source, Set<LeafNode> leaves, JsonObject object) {
		for(String key: object.keySet()) {
			JsonElement element = object.get(key);
			if(element.isJsonObject()) {
				JsonObject maybeJwks = element.getAsJsonObject();
				if(probablyJwks(maybeJwks)) {
					LeafNode wrapper = new LeafNode(source, object, element, key, LeafType.JWKS);
					leaves.add(wrapper);
					continue;
				}
				else {
					findLeafNodes(source, leaves, element.getAsJsonObject());
					continue;
				}
			}
			if(element.isJsonPrimitive()) {
				JsonPrimitive primitive = (JsonPrimitive) element;
				if(primitive.isString()) {
					LeafNode wrapper = new LeafNode(source, object, element, key, LeafType.PRIVATE_KEY);
					leaves.add(wrapper);
				}
			}
		}
	}

	private boolean probablyJwks(JsonObject maybeJwks) {
		if(maybeJwks.keySet().size() != 1) {
			return false;
		}
		if(!maybeJwks.has("keys")) {
			return false;
		}
		JsonElement keys = maybeJwks.get("keys");
		return keys.isJsonArray();
	}

}
