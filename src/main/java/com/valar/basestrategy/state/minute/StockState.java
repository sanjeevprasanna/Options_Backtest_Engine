package com.valar.basestrategy.state.minute;

public class StockState extends State{

    public StockState(String stock, String path, int parser,String dateTimeFormat,boolean removeDayIfDataNotPresent){
        super(stock,path,parser,dateTimeFormat,removeDayIfDataNotPresent);
    }

    public StockState(String stock, String path, int parser,String dateTimeFormat,int candlePeriod,boolean removeDayIfDataNotPresent){
        super(stock,path,parser,dateTimeFormat,candlePeriod,removeDayIfDataNotPresent);
    }
}
