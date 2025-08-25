package com.valar.basestrategy.tradeAndDayMetrics;



import com.valar.basestrategy.entities.Ohlc;
import com.valar.basestrategy.utils.KeyValues;
import com.valar.basestrategy.utils.PrintAttribs;
import com.valar.basestrategy.utils.PrintWriters;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.valar.basestrategy.application.PropertiesReader.properties;

public class TradeMetric {
    public int totalTrades;
    public float maxProfit = -Float.MAX_VALUE,maxLoss = Float.MAX_VALUE
            ,maxProfitPercent = -Float.MAX_VALUE,maxLossPercent = Float.MAX_VALUE;
    public ProfitLossMetric profit = new ProfitLossMetric(),
    profitPercent = new ProfitLossMetric(),
    profitWithCost = new ProfitLossMetric(),
    profitPercentWithCost = new ProfitLossMetric();

    protected float totalSell,totalBuy;
    PrintWriter orderInfo;
    public float totalHoldingPeriod;
    public double totalHoldingPeriodCost;
    private double holdingPeriodCost;


    public char lOrS;
    public Ohlc entryOhlc,exitOhlc;

    boolean isPrinting = Boolean.parseBoolean(properties.getProperty("printOrderInfo"));

    public TradeMetric(){}

    public TradeMetric(PrintWriter orderInfo){
        this.orderInfo = orderInfo;
    }

    public TradeMetric(Ohlc entryOhlc,char lOrS){
        this.entryOhlc = entryOhlc;
        this.lOrS = lOrS;
        this.orderInfo = PrintWriters.orderInfoPrintWriter;
    }

    public void calculateOverAllMetricsAndPrint(
            float profitPercent,
            float profitWithCost,
            float profitPercentWithCost,
            String reason,
            String reasonInfo,
            int id,
            int holdingPeriod,
            KeyValues kv,
            Ohlc exitOhlc,
            float indexClose,
            String option,
            PrintAttribs printAttribs,
            float tradeMaxProfit,
            double entryEma,
            double entryRsi,
            double entryPivot,
            double exitEma,
            double exitRsi,
            double exitPivot
    ){
        this.exitOhlc = exitOhlc;
        if(lOrS =='l') this.profit.add(exitOhlc.close - entryOhlc.close);
        else this.profit.add(entryOhlc.close - exitOhlc.close);

//        changes for hp related -------
        String entryDate=entryOhlc.dnt;
        String exitDate=exitOhlc.dnt;
        double holdingPeriodInMinutes=getMinutesDifference(entryDate,exitDate);
        float holdingPeriodInDay=(float) holdingPeriodInMinutes/24/60;


        this.totalHoldingPeriod+= holdingPeriodInDay;
        holdingPeriodCost=getHoldingPeriodCost(holdingPeriodInMinutes, kv.hpCostPercent);
        this.totalHoldingPeriodCost+= holdingPeriodCost;

        float afterHpCostProfitPercentCost= (float) (profitPercentWithCost-holdingPeriodCost);

        this.profitPercent.add(profitPercent);
        this.profitWithCost.add(profitWithCost);
        this.profitPercentWithCost.add(afterHpCostProfitPercentCost);



        totalTrades = 1;

        if(isPrinting){
            /*orderInfo.write(kv.sno + "," + option + "," + exitOhlc.date + "," + id + "," + holdingPeriodInDay + "," +
                    lOrS + ",Entry," + entryOhlc.date + "," + entryOhlc.time + "," +
                    entryOhlc.close+",Exit," + exitOhlc.date + "," + exitOhlc.time + "," + exitOhlc.close + "," + reason + "," +
                    reasonInfo + "," + profit.profit + "," + profitPercent +"," + tradeMaxProfit + "," +
                    profitWithCost + "," + afterHpCostProfitPercentCost + "," +
                    printAttribs + "," + indexClose  + "," + this.holdingPeriodCost + "\n");
*/
               PrintWriters.orderInfoPrintWriter.write(
                        kv.sno + "," + option + "," + exitOhlc.date + "," + id + "," + holdingPeriodInDay + "," +
                                lOrS + ",Entry," + entryOhlc.date + "," + entryOhlc.time + "," + entryOhlc.close + "," +
                                entryEma+"("+kv.emaPeriod+")" + "," + entryRsi +"("+kv.rsiLong+"-"+kv.rsiShort+")" + "," + entryPivot + "," +
                                "Exit," + exitOhlc.date + "," + exitOhlc.time + "," + exitOhlc.close + "," +
                                exitEma+"("+kv.emaPeriod+")" + "," + exitRsi+"("+kv.rsiLong+"-"+kv.rsiShort+")" + "," + exitPivot + "," +
                                reason + "," + reasonInfo + "," + profit.profit + "," + profitPercent + "," + tradeMaxProfit + "," +
                                profitWithCost + "," + afterHpCostProfitPercentCost + "," +
                                printAttribs + "," + indexClose  + "," + this.holdingPeriodCost + "\n"
                );
        PrintWriters.orderInfoPrintWriter.flush();
        }
        }
    public void calculateOverAllMetricsAndPrint(
            float profitPercent,
            float profitWithCost,
            float profitPercentWithCost,
            String reason,
            String reasonInfo,
            int id,
            int holdingPeriod,
            KeyValues kv,
            Ohlc exitOhlc,
            float indexClose,
            String option,
            PrintAttribs printAttribs,
            float tradeMaxProfit,
            double entryEma,
            double entryRsi,
            double entryPivot,
            float pdh,
            float cdh,
            double hhv,
            float pdl,
            float cdl,
            double llv,
            double exitEma,
            double exitRsi,
            double exitPivot
    ){
        this.exitOhlc = exitOhlc;
        if(lOrS =='l') this.profit.add(exitOhlc.close - entryOhlc.close);
        else this.profit.add(entryOhlc.close - exitOhlc.close);

//        changes for hp related -------
        String entryDate=entryOhlc.dnt;
        String exitDate=exitOhlc.dnt;
        double holdingPeriodInMinutes=getMinutesDifference(entryDate,exitDate);
        float holdingPeriodInDay=(float) holdingPeriodInMinutes/24/60;


        this.totalHoldingPeriod+= holdingPeriodInDay;
        holdingPeriodCost=getHoldingPeriodCost(holdingPeriodInMinutes, kv.hpCostPercent);
        this.totalHoldingPeriodCost+= holdingPeriodCost;

        float afterHpCostProfitPercentCost= (float) (profitPercentWithCost-holdingPeriodCost);

        this.profitPercent.add(profitPercent);
        this.profitWithCost.add(profitWithCost);
        this.profitPercentWithCost.add(afterHpCostProfitPercentCost);



        totalTrades = 1;

        if(isPrinting){
            /*orderInfo.write(kv.sno + "," + option + "," + exitOhlc.date + "," + id + "," + holdingPeriodInDay + "," +
                    lOrS + ",Entry," + entryOhlc.date + "," + entryOhlc.time + "," +
                    entryOhlc.close+",Exit," + exitOhlc.date + "," + exitOhlc.time + "," + exitOhlc.close + "," + reason + "," +
                    reasonInfo + "," + profit.profit + "," + profitPercent +"," + tradeMaxProfit + "," +
                    profitWithCost + "," + afterHpCostProfitPercentCost + "," +
                    printAttribs + "," + indexClose  + "," + this.holdingPeriodCost + "\n");
*/
            PrintWriters.orderInfoPrintWriter.write(
                    kv.sno + "," + option + "," + exitOhlc.date + "," + id + "," + holdingPeriodInDay + "," +
                            lOrS + ",Entry," + entryOhlc.date + "," + entryOhlc.time + "," + entryOhlc.close + "," +
                            entryEma+"("+kv.emaPeriod+")" + "," + entryRsi +"("+kv.rsiLong+"-"+kv.rsiShort+")" + "," + entryPivot + ","
                            +pdh+","+cdh+"," + hhv + ","+pdl+","+cdl +","+ llv + "," +
                            "Exit," + exitOhlc.date + "," + exitOhlc.time + "," + exitOhlc.close + "," +
                            exitEma+"("+kv.emaPeriod+")" + "," + exitRsi+"("+kv.rsiLong+"-"+kv.rsiShort+")" + "," + exitPivot + "," +
                            reason + "," + reasonInfo + "," + profit.profit + "," + profitPercent + "," + tradeMaxProfit + "," +
                            profitWithCost + "," + afterHpCostProfitPercentCost + "," +
                            printAttribs + "," + indexClose  + "," + this.holdingPeriodCost + "\n"
            );
            PrintWriters.orderInfoPrintWriter.flush();
        }
    }
    public double getHoldingPeriodCost(double mins,double hpCostPercent){
        return  (mins/24/60)*hpCostPercent;

    }

    public  double getMinutesDifference(String dnt1, String dnt2) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm");
            LocalDateTime dateTime1 = LocalDateTime.parse(dnt1, formatter);
            LocalDateTime dateTime2 = LocalDateTime.parse(dnt2, formatter);
            Duration duration = Duration.between(dateTime1, dateTime2);

            return duration.toMinutes();
        } catch (Exception e) {
            System.out.println("Error parsing date-time: " + e.getMessage());
            return -1;
        }
    }

    public void updateOverAll(TradeMetric tradeMetric){
        profit.copy(tradeMetric.profit);
        profitWithCost.copy(tradeMetric.profitWithCost);
        profitPercent.copy(tradeMetric.profitPercent);
        profitPercentWithCost.copy(tradeMetric.profitPercentWithCost);

        totalHoldingPeriod+=tradeMetric.totalHoldingPeriod;
        totalHoldingPeriodCost+=tradeMetric.holdingPeriodCost;

        if(tradeMetric.lOrS =='l') {
            totalBuy += tradeMetric.entryOhlc.close;
            totalSell += tradeMetric.exitOhlc.close;
        }else{
            totalBuy += tradeMetric.exitOhlc.close;
            totalSell += tradeMetric.entryOhlc.close;
        }

        totalTrades = 1;
    }

    public void closePrinter(){
        orderInfo.close();
    }

}
