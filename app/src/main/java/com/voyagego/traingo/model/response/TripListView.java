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
	private final String band;
	private final String sort;
	private final int limit;
	private final int total;
	private final int allCount;
	private final int fullCount;
	private final int slowCount;
	private final List<DayGroup> days;

	public TripListView(String tab, Long trainId, String query, long upcomingCount, long departedCount,
			long cancelledCount, List<TrainOption> trains, String band, String sort, int limit, int total, int allCount,
			int fullCount, int slowCount, List<DayGroup> days) {
		this.tab = tab;
		this.trainId = trainId;
		this.query = query;
		this.upcomingCount = upcomingCount;
		this.departedCount = departedCount;
		this.cancelledCount = cancelledCount;
		this.trains = List.copyOf(trains);
		this.band = band;
		this.sort = sort;
		this.limit = limit;
		this.total = total;
		this.allCount = allCount;
		this.fullCount = fullCount;
		this.slowCount = slowCount;
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
	/** "", full or slow. */
	public String getBand() { return band; }
	/** time, high or low. */
	public String getSort() { return sort; }
	public int getLimit() { return limit; }
	/** The trips the filters and the band leave, before {@link #getLimit()}. */
	public int getTotal() { return total; }
	public int getAllCount() { return allCount; }
	public int getFullCount() { return fullCount; }
	public int getSlowCount() { return slowCount; }
	public List<DayGroup> getDays() { return days; }

	/** How many "Hiện thêm" would add. */
	public int getMore() {
		return Math.min(40, total - getShown());
	}

	public int getShown() {
		return days.stream().mapToInt(day -> day.getRows().size()).sum();
	}

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

		/** "Hôm nay", "Ngày mai", "Hôm qua" or the weekday; null when the list is sorted by fill, which has no days. */
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
		private final String time;
		/** The day beside the time, only when the list has no day rows. */
		private final String dayMonth;
		private final String trainCode;
		private final String originName;
		private final String destinationName;
		private final long soldSeats;
		private final int totalSeats;
		private final int percent;
		private final String band;
		private final List<CoachFill> coaches;
		private final boolean cancelled;
		private final String phase;
		private final String phaseLabel;

		public Row(long id, String time, String dayMonth, String trainCode, String originName, String destinationName,
				long soldSeats, int totalSeats, int percent, String band, List<CoachFill> coaches, boolean cancelled,
				String phase, String phaseLabel) {
			this.id = id;
			this.time = time;
			this.dayMonth = dayMonth;
			this.trainCode = trainCode;
			this.originName = originName;
			this.destinationName = destinationName;
			this.soldSeats = soldSeats;
			this.totalSeats = totalSeats;
			this.percent = percent;
			this.band = band;
			this.coaches = coaches;
			this.cancelled = cancelled;
			this.phase = phase;
			this.phaseLabel = phaseLabel;
		}

		public long getId() { return id; }
		public String getTime() { return time; }
		public String getDayMonth() { return dayMonth; }
		public String getTrainCode() { return trainCode; }
		public String getOriginName() { return originName; }
		public String getDestinationName() { return destinationName; }
		public long getSoldSeats() { return soldSeats; }
		public int getTotalSeats() { return totalSeats; }
		public int getPercent() { return percent; }
		/** full, slow or "". */
		public String getBand() { return band; }
		public List<CoachFill> getCoaches() { return coaches; }
		public boolean isCancelled() { return cancelled; }
		/** The .tag class: scheduled, running, done or cancelled. */
		public String getPhase() { return phase; }
		public String getPhaseLabel() { return phaseLabel; }
	}

}
