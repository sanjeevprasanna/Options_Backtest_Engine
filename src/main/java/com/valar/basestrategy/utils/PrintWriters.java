package com.valar.basestrategy.utils;

import com.valar.basestrategy.application.ValarTrade;

import java.io.PrintWriter;

public class PrintWriters {
    public static PrintWriter orderInfoPrintWriter,dayWisePrintWriter,overAllPrintWriter,stockOverAllPrintWriter;

    public static void loadAllWriters()throws Exception{
        //without rsi,ema
        /*orderInfoPrintWriter = new PrintWriter("./Outputs/OrderInfo[overAll].csv");
        orderInfoPrintWriter.write("S.no,Symbol,Date,ID,Holding Period,TradeType,Event,EntryDate,EntryTime,EntryClose,Event,ExitDate,ExitTime,ExitClose,Reason,ReasonInfo,Profit,Profit%,tradeMaxProfit,ProfitWith(Cost),Profit%With(Cost),DayAtrPercentile,DayAtrPercent,candlesWaited,IndexCloseAtExit,HoldingCost" +
                "\n");*/

        //with ema,rsi,pivot
        orderInfoPrintWriter = new PrintWriter("./Outputs/OrderInfo[overAll].csv");
        orderInfoPrintWriter.write(
                "S.no,Symbol,Date,ID,Holding Period,TradeType,Event,EntryDate,EntryTime,EntryClose," +
                        "EntryEMA,EntryRSI,EntryPivot,PDH,CDH,HHV,PDL,CDL,LLV," + // New Columns
                        "Event,ExitDate,ExitTime,ExitClose," +
                        "ExitEMA,ExitRSI,ExitPivot," + // New Columns
                        "Reason,ReasonInfo,Profit,Profit%,tradeMaxProfit,ProfitWith(Cost),Profit%With(Cost)," +
                        "DayAtrPercentile,DayAtrPercent,candlesWaited,IndexCloseAtExit,HoldingCost\n"
        );

        dayWisePrintWriter = new PrintWriter("./Outputs/DayWise[overAll].csv");
        dayWisePrintWriter.write("sno,date,TotalTrades,profit,profit%,ProfitWithcost,Profit%WithCost\n");

//        dayWisePrintWriter.write(ValarTrade.keystoreHeading +",date,TotalTrades,maxProfit,maxLoss,maxProfitPercent,maxLossPercent,DayMaxProfit,DayMaxProfit%,win%,profit,profit%,cost,avgProfit,avgLoss,avgProfit%,avgLoss%,TradeExpectancy\n");

        String overallHeading = ValarTrade.keystoreHeading+",TradingDays,TotalTrades,TradeMaxProfit,TradeMaxLoss" +
                ",DayMaxProfit,DayMaxLoss,TradeAverageProfit,TradeAverageLoss,TradeWinPercent,TradeWinPercent(Cost),TradeExpectancy,TradeExpectancy(Cost),Profit,ProfitPercent" +
                ",DayAverageProfit,DayAverageLoss,DayWinPercent,DayExpectancy,Profit(Cost),ProfitPercent(Cost),DayWinPercent(Cost)" +
                ",DayAverageProfit(Cost),DayAverageLoss(Cost),DayExpectancy(Cost),HoldingPeriodAvg,Calmar,TotalHpCost,profitPerTrade(Cost)\n";
        overAllPrintWriter = new PrintWriter("./Outputs/overAllDetails[serialWise].csv");
        overAllPrintWriter.write(overallHeading);


        stockOverAllPrintWriter = new PrintWriter("./Outputs/OverAllDetails[Stockwise].csv");
        stockOverAllPrintWriter.write("Stock,"+overallHeading);
    }

    public static void closeAllWriters(){
        orderInfoPrintWriter.close();
        dayWisePrintWriter.close();
        overAllPrintWriter.close();
        stockOverAllPrintWriter.close();
    }
}
