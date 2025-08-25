package com.valar.basestrategy.state.day;

public class OptionDayState extends DayState{
    public OptionDayState(String name, String path, int parser, String dateTimeFormat) {
        super(name, path, parser, dateTimeFormat);
    }

    public OptionDayState(String name, String path, String readTill, String dateTimeFormat) {
        super(name, path, readTill, dateTimeFormat);
    }
}
