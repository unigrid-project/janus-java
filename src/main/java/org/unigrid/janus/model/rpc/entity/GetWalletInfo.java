package org.unigrid.janus.model.rpc.entity;

import jakarta.json.bind.annotation.JsonbProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class GetWalletInfo extends BaseResult<GetWalletInfo.Result> {

	public static final String METHOD = "getwalletinfo";

	public static class Request extends BaseRequest {

		public Request() {
			super(METHOD);
		}
	}

	@Data
	public static class Result {
		private float balance;
		@JsonbProperty("unlocked_until")
		private long unlockUntil = 4999;
	}
}
