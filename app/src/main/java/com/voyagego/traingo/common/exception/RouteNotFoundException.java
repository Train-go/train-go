package com.voyagego.traingo.common.exception;

public class RouteNotFoundException extends RuntimeException {
	public RouteNotFoundException(long id) {
		super("Không tìm thấy tuyến " + id + ".");
	}
}
