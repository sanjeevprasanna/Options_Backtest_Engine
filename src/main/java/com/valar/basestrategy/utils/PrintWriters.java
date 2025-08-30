package com.valar.basestrategy.utils;

import com.valar.basestrategy.application.ValarTrade;

import java.io.PrintWriter;

public class PrintWriters {
    public static PrintWriter orderInfoPrintWriter, dayWisePrintWriter, overAllPrintWriter, stockOverAllPrintWriter, optionsStratWriter;

    public static void loadAllWriters() throws Exception {
        // Standard writers (unchanged)
        orderInfoPrintWriter = new PrintWriter("./Outputs/OrderInfo[overAll].csv");
        orderInfoPrintWriter.write(
                "S.no,Symbol,Date,ID,Holding Period,TradeType,Event,EntryDate,EntryTime,EntryClose," +
                        "EntryEMA,EntryRSI,EntryPivot,PDH,CDH,HHV,PDL,CDL,LLV," +
                        "Event,ExitDate,ExitTime,ExitClose," +
                        "ExitEMA,ExitRSI,ExitPivot," +
                        "Reason,ReasonInfo,Profit,Profit%,tradeMaxProfit,ProfitWith(Cost),Profit%With(Cost)," +
                        "DayAtrPercentile,DayAtrPercent,candlesWaited,IndexCloseAtExit,HoldingCost\n"
        );

        dayWisePrintWriter = new PrintWriter("./Outputs/DayWise[overAll].csv");
        dayWisePrintWriter.write("sno,date,TotalTrades,profit,profit%,ProfitWithcost,Profit%WithCost\n");

        String overallHeading = ValarTrade.keystoreHeading + ",TradingDays,TotalTrades,TradeMaxProfit,TradeMaxLoss" +
                ",DayMaxProfit,DayMaxLoss,TradeAverageProfit,TradeAverageLoss,TradeWinPercent,TradeWinPercent(Cost),TradeExpectancy,TradeExpectancy(Cost),Profit,ProfitPercent" +
                ",DayAverageProfit,DayAverageLoss,DayWinPercent,DayExpectancy,Profit(Cost),ProfitPercent(Cost),DayWinPercent(Cost)" +
                ",DayAverageProfit(Cost),DayAverageLoss(Cost),DayExpectancy(Cost),HoldingPeriodAvg,Calmar,TotalHpCost,profitPerTrade(Cost)\n";
        overAllPrintWriter = new PrintWriter("./Outputs/overAllDetails[serialWise].csv");
        overAllPrintWriter.write(overallHeading);

        stockOverAllPrintWriter = new PrintWriter("./Outputs/OverAllDetails[Stockwise].csv");
        stockOverAllPrintWriter.write("Stock," + overallHeading);

        // OptionsStrat - only write on exit (see method below)
        optionsStratWriter = new PrintWriter("./Outputs/OptionsStrat.csv");
        optionsStratWriter.write(
                "Symbol,EntryDate,EntryTime,EntryIndex,EntryClose,ADX,PlusDI,MinusDI,"
                        + "ATM_Strike,ATM_Entry,ATM_Exit,OTM_Strike,OTM_Entry,OTM_Exit,"
                        + "Expiry,Strategy,LotSize,"
                        + "ExitDate,ExitTime,ExitIndex,ExitClose,"
                        + "TradeProfit,TradeProfit%,TradeProfitWithCost,TradeProfit%WithCost,"
                        + "HoldingPeriod,IndexCloseAtExit\n"
        );
    }

    public static void closeAllWriters() {
        orderInfoPrintWriter.close();
        dayWisePrintWriter.close();
        overAllPrintWriter.close();
        stockOverAllPrintWriter.close();
        optionsStratWriter.close();
    }

    public static void writeOptionsTradeExit(
            String symbol, String entryDate, String entryTime, int entryIndex, double entryClose,
            double adx, double plusDi, double minusDi,
            int atmStrike, float atmEntry, float atmExit,
            int otmStrike, float otmEntry, float otmExit,
            String expiry, String strategy, int lotSize,
            String exitDate, String exitTime, int exitIndex, double exitClose,
            float tradeProfit, float tradeProfitPercent, float tradeProfitWithCost, float tradeProfitPercentWithCost,
            double holdingPeriod, double indexCloseAtExit
    ) {
        String[] indexName = symbol.trim().split("\\s+", 2);
        if (optionsStratWriter != null) {
            optionsStratWriter.printf(
                    "%s,%s,%s,%d,%.2f,%.4f,%.4f,%.4f,"
                            + "%d,%.2f,%.2f,%d,%.2f,%.2f,"
                            + "%s,%s,%d,"
                            + "%s,%s,%d,%.2f,"
                            + "%.2f,%.2f,%.2f,%.2f,"
                            + "%.2f,%.2f\n",
                    indexName[0], entryDate, entryTime, entryIndex, entryClose, adx, plusDi, minusDi,
                    atmStrike, atmEntry, atmExit, otmStrike, otmEntry, otmExit,
                    expiry, strategy, lotSize,
                    exitDate, exitTime, exitIndex, exitClose,
                    tradeProfit, tradeProfitPercent, tradeProfitWithCost, tradeProfitPercentWithCost,
                    holdingPeriod, indexCloseAtExit
            );
            optionsStratWriter.flush();
        }
    }
}