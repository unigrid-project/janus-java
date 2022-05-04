package org.unigrid.janus.model.rpc.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class GetBlockCount extends BaseResult<String> {

	public static final String METHOD = "getblockcount";

	public static class Request extends BaseRequest {

		public Request() {
			super(METHOD);
		}
	}

}
