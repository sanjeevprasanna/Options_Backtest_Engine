package com.valar.basestrategy.tradeAndDayMetrics;

import com.valar.basestrategy.utils.PrintWriters;

public class DayMetric extends TradeMetric {
    public float dayMaxProfit,dayMaxProfitPercent;
    private float costPercent,bnCloseAtEntry,cost;
    public int totalTrades;
    String date;
    private int sno; 

    public ProfitLossMetric tradeProfit = new ProfitLossMetric(),
    tradeProfitPercent = new ProfitLossMetric(),
    tradeProfitWithCost = new ProfitLossMetric(),
    tradeProfitPercentWithCost = new ProfitLossMetric();


    public DayMetric(String date,float costPercent,float bnCloseAtEntry,int sno){
        super(PrintWriters.dayWisePrintWriter);
        this.date = date;
        this.costPercent = costPercent;
        this.bnCloseAtEntry = bnCloseAtEntry;
        this.sno=sno;
    }

    public void print(){
        orderInfo.write(sno+","+date+","+ totalTrades +","+profit.profit
                +","+ profitPercent.profit +","+profitWithCost.profit+","+profitPercentWithCost.profit+",\n");

    }

    private void updateMaxMinProfitsOnTradeExit(TradeMetric tradeMetric){
        maxProfit = Float.max(maxProfit,tradeMetric.profit.profit);
        maxLoss = Float.min(maxLoss,tradeMetric.profit.profit);
        maxProfitPercent = Float.max(maxProfitPercent,tradeMetric.profitPercent.profit);
        maxLossPercent = Float.min(maxLossPercent,tradeMetric.profitPercent.profit);
    }

    public void updateMetric(TradeMetric overAllTradeMetric,float dayMaxProfit,float dayMaxProfitPercent){
        totalHoldingPeriod+= overAllTradeMetric.totalHoldingPeriod;
        totalHoldingPeriodCost+= overAllTradeMetric.totalHoldingPeriodCost;


        totalSell += overAllTradeMetric.totalSell;
        totalBuy += overAllTradeMetric.totalBuy;


        this.dayMaxProfit = Float.max(this.dayMaxProfit,dayMaxProfit);
        this.dayMaxProfitPercent = Float.max(this.dayMaxProfitPercent,dayMaxProfitPercent);
        updateMaxMinProfitsOnTradeExit(overAllTradeMetric);

        tradeProfit.copy(overAllTradeMetric.profit);
        tradeProfitWithCost.copy(overAllTradeMetric.profitWithCost);
        tradeProfitPercent.copy(overAllTradeMetric.profitPercent);
        tradeProfitPercentWithCost.copy(overAllTradeMetric.profitPercentWithCost);


        profit.add(overAllTradeMetric.profit.profit);
        profitWithCost.add(overAllTradeMetric.profitWithCost.profit);
        profitPercent.add(overAllTradeMetric.profitPercent.profit);
        profitPercentWithCost.add(overAllTradeMetric.profitPercentWithCost.profit);

        totalTrades++;

    }

}
