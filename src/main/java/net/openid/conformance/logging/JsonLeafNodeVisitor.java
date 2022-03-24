package net.openid.conformance.logging;

public interface JsonLeafNodeVisitor {
	void accept(JsonObjectSanitiser.LeafNode leafNode);
}
