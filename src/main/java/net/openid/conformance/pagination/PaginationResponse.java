package net.openid.conformance.pagination;

import java.util.List;

public class PaginationResponse<T> {

	public final int draw;
	public final long recordsTotal;
	public final long recordsFiltered;
	public final List<T> data;

	PaginationResponse(int draw, long recordsTotal, long recordsFiltered, List<T> data) {
		this.draw = draw;
		this.recordsTotal = recordsTotal;
		this.recordsFiltered = recordsFiltered;
		this.data = data;
	}
}
