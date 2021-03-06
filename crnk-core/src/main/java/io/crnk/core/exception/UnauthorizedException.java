package io.crnk.core.exception;

import io.crnk.core.engine.document.ErrorData;
import io.crnk.core.engine.http.HttpStatus;

public class UnauthorizedException extends CrnkMappableException {  // NOSONAR exception hierarchy deep but ok

	private static final String TITLE = "UNAUTHORIZED";

	public UnauthorizedException(String message) {
		super(HttpStatus.UNAUTHORIZED_401, ErrorData.builder().setTitle(TITLE).setDetail(message)
				.setStatus(String.valueOf(HttpStatus.UNAUTHORIZED_401)).build());
	}
}
