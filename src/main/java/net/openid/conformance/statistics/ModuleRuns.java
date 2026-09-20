package net.openid.conformance.statistics;

import java.util.List;

/**
 * What the modules pipeline hands the cube: the module cells, and who the owner ids on them
 * stand for.
 *
 * @param cells  runs of each test module per month and user
 * @param owners the users of those cells; a cell's {@link ModuleUserCell#ownerId()} is an
 *               index into this list
 */
public record ModuleRuns(List<ModuleUserCell> cells, List<Owner> owners) {

	/** No module was run. */
	public static final ModuleRuns NONE = new ModuleRuns(List.of(), List.of());

	/** @throws IllegalArgumentException if a cell's owner id is not an index into {@code owners} */
	public ModuleRuns {
		cells = List.copyOf(cells);
		owners = List.copyOf(owners);
		for (ModuleUserCell cell : cells) {
			if (cell.ownerId() < 0 || cell.ownerId() >= owners.size()) {
				throw new IllegalArgumentException("Module cell of " + cell.testName() + " in " + cell.month()
					+ " has owner id " + cell.ownerId() + ", but there are " + owners.size() + " owners");
			}
		}
	}
}
