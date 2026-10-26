package com.voyagego.traingo.common.exception;

/**
 * The train still has trips that are not cancelled and have not arrived, so
 * it cannot be deleted: customers may hold tickets for them.
 */
public class TrainHasUnfinishedTripsException extends BusinessRuleException {

	public TrainHasUnfinishedTripsException(long tripCount) {
		super("Không xóa được: tàu còn " + tripCount + " chuyến sắp chạy hoặc đang chạy.");
	}

}
