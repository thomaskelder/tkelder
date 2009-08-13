package org.wikipathways.stats;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.jfree.data.time.RegularTimePeriod;

public class TimeInterval {
	Date start;
	Date end;
	RegularTimePeriod curr;
	Class<? extends RegularTimePeriod> period;
	
	public TimeInterval(Date start, Class<? extends RegularTimePeriod> period) {
		this(start, Calendar.getInstance().getTime(), period);
	}
	
	public TimeInterval(Date start, Date end, Class<? extends RegularTimePeriod> period) {
		this.start = start;
		this.end = end;
		this.period = period;
	}
	
	public Date getEnd() {
		return end;
	}
	
	public Date getStart() {
		return start;
	}
	
	private RegularTimePeriod create(Date start) {
		return RegularTimePeriod.createInstance(period, start, TimeZone.getTimeZone("GMT"));
	}
	
	public RegularTimePeriod getNext() {
		curr = curr == null ? create(start) : curr.next();
		Date middle = new Date(curr.getMiddleMillisecond());
		if(middle.before(end)) {
			return curr;
		} else {
			return null;
		}
	}
	
	public enum Interval {
		DAY, WEEK, MONTH
	}
}
