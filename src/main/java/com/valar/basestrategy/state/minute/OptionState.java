package com.valar.basestrategy.state.minute;

public class OptionState extends State {
    public String peOrCE,optionPrefix;
    public float premium;
    private int strike;
    public boolean belongsToBN;

    public OptionState(String optionPrefix,String option, String path, int parser,String dateTimeFormat,boolean removeDayIfDataNotPresent){
        super(option,path,parser,dateTimeFormat,removeDayIfDataNotPresent);
        load(optionPrefix,option);
    }

    public OptionState(String optionPrefix,String option, String path,boolean readB4, String readB4OrTillDnt,String dateTimeFormat,boolean removeDayIfDataNotPresent){
        super(option,path,readB4,readB4OrTillDnt,dateTimeFormat,removeDayIfDataNotPresent);
        load(optionPrefix,option);
    }

    public OptionState(String optionPrefix,String option, String path, int parser,String dateTimeFormat,int candlePeriod,boolean removeDayIfDataNotPresent){
        super(option,path,parser,dateTimeFormat,candlePeriod,removeDayIfDataNotPresent);
        load(optionPrefix,option);
    }

    public OptionState(String optionPrefix,String option, String path, String readTill,String dateTimeFormat,int candlePeriod,boolean removeDayIfDataNotPresent){
        super(option,path,readTill,dateTimeFormat,candlePeriod,removeDayIfDataNotPresent);
        load(optionPrefix,option);
    }

    private void load(String optionPrefix,String option){
        this.optionPrefix = optionPrefix;
        if(option.contains("PE") || option.contains("CE"))peOrCE = option.substring(option.length()-6, option.length()-4);
        if(option.startsWith("BANKNIFTY"))belongsToBN = true;
        if(optionPrefix!=null)strike = Integer.parseInt(
                option.replace(optionPrefix,"")
                        .replace(peOrCE+".csv",""));
    }

    public int getStrike(){
        return strike;
    }


    public String toString(){
        return name;
    }
}
