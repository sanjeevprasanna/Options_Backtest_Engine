package com.valar.basestrategy.tradeAndDayMetrics;

public class ProfitLossMetric implements Cloneable {
    public int tradesOrDays;
    public float profit,profitOfProfitables,lossOfLosables;
    public int inProfit,inLoss;

    public void add(float profit){
        this.profit += profit;
        tradesOrDays++;
        if(profit>=0){
            inProfit++;
            profitOfProfitables+=profit;
        } else{
            inLoss++;
            lossOfLosables+=profit;
        }
    }

    public void copy(ProfitLossMetric other){
        this.tradesOrDays += other.tradesOrDays;
        this.profit += other.profit;
        this.inProfit += other.inProfit;
        this.inLoss += other.inLoss;
        this.profitOfProfitables += other.profitOfProfitables;
        this.lossOfLosables += other.lossOfLosables;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();  // Call to Object's clone() method
    }

    public float getWinPercent(){
        return (float) inProfit / tradesOrDays * 100;
    }

    public float getAvgProfit(){
        return profitOfProfitables / inProfit;
    }

    public float getAvgLoss(){
        return lossOfLosables / inLoss;
    }

    public float getExpectancy(){
        float avgProfit = getAvgProfit(),avgLoss = getAvgLoss(),
        winPercent = getWinPercent();
        return (-avgProfit/avgLoss) * (winPercent/(100-winPercent));
    }
}
