package com.pardot.rhombus.cobject.shardingstrategy;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;

public class ShardingStrategyWeekly extends TimebasedShardingStrategy {

    public ShardingStrategyWeekly(){
    }

    public long getShardKey(long timestamp){
        DateTime d = new DateTime(timestamp, DateTimeZone.UTC);
        DateTime start = new DateTime(2000, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);
        int daysSinceEpoch = Days.daysBetween(start, d).getDays();
        return this.offset + (daysSinceEpoch / 7);
    }
}
