package com.valar.basestrategy.state.day;

public class IndexDayState extends DayState{
    public IndexDayState(String name, String path, int parser, String dateTimeFormat) {
        super(name, path, parser, dateTimeFormat);
    }

    public IndexDayState(String name, String path, String readTill, String dateTimeFormat) {
        super(name, path, readTill, dateTimeFormat);
    }
}
