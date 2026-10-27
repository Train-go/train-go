package com.voyagego.traingo.model.response;

import java.util.List;

/**
 * The /admin/trips page: the rows of the selected group, split by departure
 * day, and the size of each of the three groups (D8). The counts ignore the
 * train and station filters, so a tab always shows how big its group is.
 */
public class TripListView {

	private final String tab;
	private final Long trainId;
	private final String query;
	private final long upcomingCount;
	private final long departedCount;
	private final long cancelledCount;
	private final List<TrainOption> trains;
	private final List<DayGroup> days;

	public TripListView(String tab, Long trainId, String query, long upcomingCount, long departedCount,
			long cancelledCount, List<TrainOption> trains, List<DayGroup> days) {
		this.tab = tab;
		this.trainId = trainId;
		this.query = query;
		this.upcomingCount = upcomingCount;
		this.departedCount = departedCount;
		this.cancelledCount = cancelledCount;
		this.trains = List.copyOf(trains);
		this.days = List.copyOf(days);
	}

	/** upcoming, departed or cancelled. */
	public String getTab() { return tab; }
	public Long getTrainId() { return trainId; }
	public String getQuery() { return query; }
	public long getUpcomingCount() { return upcomingCount; }
	public long getDepartedCount() { return departedCount; }
	public long getCancelledCount() { return cancelledCount; }
	public List<TrainOption> getTrains() { return trains; }
	public List<DayGroup> getDays() { return days; }

	public boolean isFiltered() {
		return trainId != null || query != null;
	}

	public static final class TrainOption {

		private final long id;
		private final String code;
		private final String name;

		public TrainOption(long id, String code, String name) {
			this.id = id;
			this.code = code;
			this.name = name;
		}

		public long getId() { return id; }
		public String getCode() { return code; }
		public String getName() { return name; }
	}

	/** The header row of one departure day and its trips. */
	public static final class DayGroup {

		/** "Hôm nay", "Ngày mai", "Hôm qua" or the weekday. */
		private final String label;
		private final String date;
		private final List<Row> rows;

		public DayGroup(String label, String date, List<Row> rows) {
			this.label = label;
			this.date = date;
			this.rows = List.copyOf(rows);
		}

		public String getLabel() { return label; }
		public String getDate() { return date; }
		public List<Row> getRows() { return rows; }
	}

	public static final class Row {

		private final long id;
		/** For example "08:00 → 16:45", with the arrival date when it is a later day. */
		private final String time;
		private final String trainCode;
		private final String route;
		private final long basePrice;
		private final long soldSeats;
		private final int totalSeats;
		private final String phase;
		private final String phaseLabel;

		public Row(long id, String time, String trainCode, String route, long basePrice, long soldSeats,
				int totalSeats, String phase, String phaseLabel) {
			this.id = id;
			this.time = time;
			this.trainCode = trainCode;
			this.route = route;
			this.basePrice = basePrice;
			this.soldSeats = soldSeats;
			this.totalSeats = totalSeats;
			this.phase = phase;
			this.phaseLabel = phaseLabel;
		}

		public long getId() { return id; }
		public String getTime() { return time; }
		public String getTrainCode() { return trainCode; }
		public String getRoute() { return route; }
		public long getBasePrice() { return basePrice; }
		public long getSoldSeats() { return soldSeats; }
		public int getTotalSeats() { return totalSeats; }
		/** The .tag class: scheduled, running, done or cancelled. */
		public String getPhase() { return phase; }
		public String getPhaseLabel() { return phaseLabel; }
	}

}
