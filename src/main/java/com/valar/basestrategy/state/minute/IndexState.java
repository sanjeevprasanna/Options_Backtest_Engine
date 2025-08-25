package com.valar.basestrategy.state.minute;

public class IndexState extends State{
    public IndexState(String name, String path, int parser,String dateTimeFormat,boolean removeDayIfDataNotPresent){
        super(name,path,parser,dateTimeFormat,removeDayIfDataNotPresent);
    }

    public IndexState(String name, String path, int parser,String dateTimeFormat,int candlePeriod,boolean removeDayIfDataNotPresent){
        super(name,path,parser,dateTimeFormat,candlePeriod,removeDayIfDataNotPresent);
    }
}
