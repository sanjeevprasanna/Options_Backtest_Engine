package com.valar.basestrategy.utils;

public class PrintAttribs {
    private double dayAtrPercent;
    private double dayAtrPercentage;
    private int waitingModeCheckCount;

    public void setVariablesAtEntry(double dayAtrPercent,double dayAtrPercentage,int waitingModeCheckCount){
        this.dayAtrPercent = dayAtrPercent;
        this.dayAtrPercentage = dayAtrPercentage;
        this.waitingModeCheckCount = waitingModeCheckCount;
    }

    @Override
    public String toString() {
        return dayAtrPercent +
                "," + dayAtrPercentage+"," + waitingModeCheckCount;
    }
}
