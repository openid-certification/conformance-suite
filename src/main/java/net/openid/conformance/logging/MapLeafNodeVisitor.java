package net.openid.conformance.logging;

public interface MapLeafNodeVisitor {

	void accept(MapSanitiser.LeafNode leafNode);

}
