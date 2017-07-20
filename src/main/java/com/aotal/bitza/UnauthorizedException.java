package com.aotal.bitza;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = org.springframework.http.HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {

	public UnauthorizedException() {
		// TODO Auto-generated constructor stub
	}

	public UnauthorizedException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public UnauthorizedException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public UnauthorizedException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public UnauthorizedException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
