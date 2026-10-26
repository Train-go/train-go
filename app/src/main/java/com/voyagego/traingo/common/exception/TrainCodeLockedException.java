package com.voyagego.traingo.common.exception;

/**
 * The train already has trips, so its code can no longer change: tickets and
 * the admin's lists already show the train under that code.
 */
public class TrainCodeLockedException extends BusinessRuleException {

	public TrainCodeLockedException() {
		super("Mã tàu không đổi được khi tàu đã có chuyến.", "code");
	}

}
