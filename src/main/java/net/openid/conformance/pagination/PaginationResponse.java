package net.openid.conformance.pagination;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "DataTables-style pagination envelope")
public class PaginationResponse<T> {

	@Schema(description = "The 'draw' value from the request, echoed back")
	public final int draw;
	@Schema(description = "Synthetic count, not a true total: start + length + 1 when a further page exists, otherwise start plus the number of records returned")
	public final long recordsTotal;
	@Schema(description = "Always the same value as recordsTotal (the suite does not compute a separate filtered count)")
	public final long recordsFiltered;
	@Schema(description = "The page of records; the item type depends on the endpoint")
	public final List<T> data;

	PaginationResponse(int draw, long recordsTotal, long recordsFiltered, List<T> data) {
		this.draw = draw;
		this.recordsTotal = recordsTotal;
		this.recordsFiltered = recordsFiltered;
		this.data = data;
	}
}
