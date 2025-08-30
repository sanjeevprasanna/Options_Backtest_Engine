package com.valar.basestrategy.service;
import static com.valar.basestrategy.application.PropertiesReader.properties;
import static com.valar.basestrategy.utils.ValarUtils.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.valar.basestrategy.entities.Ohlc;
import com.valar.basestrategy.state.minute.IndexState;
import com.valar.basestrategy.state.minute.State;
import com.valar.basestrategy.tradeAndDayMetrics.DayMetric;
import com.valar.basestrategy.tradeAndDayMetrics.OverAllMetric;
import com.valar.basestrategy.utils.KeyValues;

public class Strategy {
    private String dateTimeFormat = "dd-MM-yy HH:mm";
    private DateFormat dateFormat = new SimpleDateFormat("dd-MM-yy");
    private boolean positional,candlePeriodBelongsToDay;
    private int candlePeriod;
    String vixFilePath =properties.getProperty("vixFilePath");
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yy");
    private static String[] stocksFolders;
    public static String runTill = properties.getProperty("runTill")+" 15:30";

    static{
        int[] periods = {1,5,15,30,45,60,75,375};
        stocksFolders = Arrays.stream(periods).mapToObj(p -> {
            String cleanedOrDownloaded = "Cleaned",fromDate = "20150202";
            if(p==375){
                fromDate = "20000101";
                cleanedOrDownloaded = "downloaded";
            }
            return properties.getProperty("stocksBaseFolderPath") +"Stocks "+p+"min "+fromDate+" to 20250124 "+cleanedOrDownloaded+" EndTime/";
        }).toArray(String[]::new);
    }


    class OverAllMetricInfo{
        public OverAllMetric overAllMetric = new OverAllMetric();
        public Map<String,OverAllMetric> stockOverAllMetric = new HashMap<>();
        public Map<String,DayMetric> dayMetricsMap = new LinkedHashMap<>();
        public Map<String,Map<String,DayMetric>> stockDayMetricsMap = new HashMap<>();
        String keystoreLn;
        float costPercent;
        public OverAllMetricInfo(String keystoreLn,float costPercent){
            this.keystoreLn = keystoreLn;
            this.costPercent = costPercent;
        }

        public void addStockDayMetricMap(String symbol){
            symbol = symbol.contains(" ")?symbol.split(" ")[0]:symbol;
            stockDayMetricsMap.put(symbol,new LinkedHashMap<String,DayMetric>());
            stockOverAllMetric.put(symbol,new OverAllMetric(symbol));
        }
    }

    public Map<Integer,OverAllMetricInfo> overAllMetricInfoMap = new HashMap<>();
    private List<KeyValues> runForKeyAttribs;

    private Set<Integer> globakCandlePeriodsInKeystore = new HashSet<>();

    public Strategy(boolean positional,int candlePeriod,List<KeyValues> runForKeyAttribs){
        this.positional = positional;
        this.candlePeriod = candlePeriod;
        this.runForKeyAttribs = runForKeyAttribs;
        for(KeyValues kv : runForKeyAttribs){
            overAllMetricInfoMap.put(kv.sno,new OverAllMetricInfo(kv.ln,kv.costPercent));
            globakCandlePeriodsInKeystore.addAll(List.of(kv.candlePeriod));
        }
        candlePeriodBelongsToDay = candlePeriod==375;

    }

    /*private void continueFurtherReadingForSameExpiryFiles(String date,Map<String,OptionState> allOptionStates){
        for(OptionState os : allOptionStates.values())
            os.readTillDate(false,true,date+" 09:15");
    }*/

    public void apply()throws Exception{
        String stocksMinFilesPath = stocksFolders[0],
                stocksDayFilesPath = properties.getProperty("stocksDayFilesPath");
        List<String> runForInstruments = Files.readAllLines(Paths.get(properties.getProperty("instrumentsFile")));
        List<String> files = new ArrayList<>(getAllFilesOfFolder(stocksMinFilesPath));
        files.add(indexFile.split("/")[1]);
        files.stream().filter(file->(runForInstruments.contains("BankNifty") && file.startsWith("BankNifty"))
                        || runForInstruments.contains(file.replace(".csv","")))
                .forEach(file -> {
                    boolean isIndex = file.startsWith("Bank");
                    String dayFilePath = isIndex?indexDayFile:stocksDayFilesPath+file;
                    Map<Integer, IndexState> indexStateMap = new HashMap<>();

                    if(isIndex) {
                        String bnBaseFolderPath = properties.getProperty("" +
                                "indexFilePath");
                        List<String> periodicFiles = getAllFilesOfFolder(bnBaseFolderPath);
                        periodicFiles.stream().filter(pf -> pf.startsWith("BankNifty") && pf.contains("min"))
                                .forEach(pf -> {
                                    int duration = Integer.parseInt(pf.split(" ")[1].replace("min", ""));
                                    if(globakCandlePeriodsInKeystore.contains(duration)) {
                                        indexStateMap.put(
                                                duration,
                                                new IndexState(pf.replace(".csv", ""), bnBaseFolderPath + pf, 0, dateTimeFormat, true)
                                        );
                                    }
                                });
                    }else{

                        Arrays.stream(stocksFolders).forEach(bf -> {
                            int duration = Integer.parseInt(bf.split(" ")[1].replace("min", ""));
                            if(globakCandlePeriodsInKeystore.contains(duration)) {
                                indexStateMap.put(
                                        duration,
                                        new IndexState(file.replace(".csv", ""), bf + file, 0, dateTimeFormat, true)
                                );
                            }
                        });
                    }

                    indexStateMap.entrySet().stream().filter(entry->entry.getKey()==375)
                            .forEach(entry->{
                                entry.getValue().readTillDate(false,true,"09-01-15 15:29");
                            });

                    for(KeyValues kv : runForKeyAttribs) {
                        overAllMetricInfoMap.get(kv.sno).addStockDayMetricMap(file.replace(".csv",""));

                        IndexState indexState1 = indexStateMap.get(kv.candlePeriod); //15 35000 data

                        indexState1.loadIndicators(kv.emaPeriod,kv.rsiPeriod,kv.adxPeriod,kv.adxSmoothing);


                    }
                    Map<String, Double> dayAtrMap = new HashMap<>();
                    Map<String , Double > dayAtrMapPercentage=new HashMap<>();

                    try {
                        dayAtrMapPercentage = Files.readAllLines(Paths.get(dayFilePath)).stream()
//                                .skip(14)
                                .map(line -> line.split(",")) // Split by comma (assuming CSV format)
                                .collect(Collectors.toMap(
                                        cols -> cols[0],               // Key: date column
                                        cols -> Double.parseDouble(cols[6])
                                ));
                    } catch(Exception e){e.printStackTrace();}

                    try {
                        dayAtrMap = Files.readAllLines(Paths.get(dayFilePath)).stream()
                                .map(line -> line.split(",")) // Split by comma (assuming CSV format)
                                .collect(Collectors.toMap(
                                        cols -> cols[0],               // Key: date column
                                        cols -> isIndex?Double.parseDouble(cols[6]):Double.parseDouble(cols[7])
                                ));
                    } catch(Exception e){e.printStackTrace();}

                    IndexState indexState = indexStateMap.get(candlePeriod);
                    applyStrategy(indexStateMap,indexState,dayAtrMap,dayAtrMapPercentage);
                });
    }


    public void applyStrategy(Map<Integer, IndexState> indexStateMap,State indexState,Map<String,Double> dayAtrMap,Map<String ,Double> dayAtrMapPercentage){
        Ohlc indexOhlc = indexState.ohlc,indexNextOhlc = indexState.nextOhlc;

        List<StrategyImpl> strategyImpls = new ArrayList<>();
//        int maxVolPeriod = -Integer.MAX_VALUE;
        for(KeyValues kv : runForKeyAttribs){
            String key = indexState.name.contains(" ")?indexState.name.split(" ")[0]:indexState.name;
            Map<String, DayMetric> map = overAllMetricInfoMap.get(kv.sno).stockDayMetricsMap.get(key);
            strategyImpls.add(new StrategyImpl(candlePeriodBelongsToDay,indexStateMap,kv,dayAtrMap,dayAtrMapPercentage,indexState,overAllMetricInfoMap.get(kv.sno).dayMetricsMap,map));
//            maxVolPeriod = Math.max(maxVolPeriod, kv.volumeLookBack);
        }
//        indexState.ohlc.storeVolumesForPeriod(maxVolPeriod+1);

        do {
            indexState.getNext(true);
            indexStateMap.forEach((key, is) -> {
                int period = key;
                if (period != candlePeriod){
                    if(period!=375 && indexOhlc.date.equals(is.nextOhlc.date) && is.nextOhlc.mins <= indexOhlc.mins) {
                        is.getNext(true);
                    }else if(period == 375){
                        if(indexOhlc.dnt.equals(is.nextOhlc.dnt)){
                            is.getNext(true);
                        }
                        else if(!indexOhlc.date.equals(is.nextOhlc.date)){// for handling extra dates present in dayData but not in minuteData
                            // Parse the strings into LocalDate objects
                            LocalDate indexDate = LocalDate.parse(indexOhlc.date, formatter),
                                    dayDate = LocalDate.parse(is.nextOhlc.date, formatter);
                            if(indexDate.isAfter(dayDate)){
                                while(indexDate.isAfter(dayDate)){
                                    is.getNext(true);
                                    indexDate = LocalDate.parse(indexOhlc.date, formatter);
                                    dayDate = LocalDate.parse(is.nextOhlc.date, formatter);
                                }
                            }
                        }
                    }
                }
            });


            int mins = indexOhlc.mins;

            if(indexOhlc.close!=0) {
                strategyImpls.forEach(strategyImpl -> {
                    if(!strategyImpl.dayExited)
                        //System.out.println("Iterate entered");
                        strategyImpl.iterate(mins);
                });
                //System.out.println(indexOhlc.dnt+" ");
            }

            if(!positional && (!indexOhlc.date.equals(indexNextOhlc.date) || indexState.finished)) {
                strategyImpls.clear();
                for(KeyValues kv : runForKeyAttribs){
                    String key = indexState.name.contains(" ")?indexState.name.split(" ")[0]:indexState.name;
                    Map<String, DayMetric> map = overAllMetricInfoMap.get(kv.sno).stockDayMetricsMap.get(key);
                    strategyImpls.add(new StrategyImpl(candlePeriodBelongsToDay,indexStateMap,kv,dayAtrMap,dayAtrMapPercentage,indexState,overAllMetricInfoMap.get(kv.sno).dayMetricsMap,map));
                }
            }

//            System.out.println(indexOhlc.date+" "+runTill);
        }while(!indexState.finished && !runTill.equals(indexOhlc.dnt));
    }


    private List<String> getKeyValuesAsString(String file,int sno){
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            br.readLine();
            String s;
            List<String> ifNoneFound = new ArrayList<>();
            boolean found = false;
            List<String> list = new ArrayList();
            while ((s = br.readLine()) != null) {
                int hsno = Integer.parseInt(s.split(",")[0]);
                if (!found && hsno == sno) found = true;
                if (hsno == sno || hsno == 2000)
                    list.add(s);
                else if (hsno == 1000)
                    ifNoneFound.add(s);
            }

            if (list.size() == 0 || !found)
                for (String ss : ifNoneFound) list.add(ss);

            if (list.size() == 0) {
                System.err.println("No KeyStore Value found in "+file+" for keystore "+sno);
                System.exit(0);
            }
//            System.out.println("For "+file+" ks is "+list);

            return list;
        }catch (Exception e){ e.printStackTrace();System.exit(0); }

        return null;
    }

    private void processDayMetricMap(boolean printDayWise,Map<String,DayMetric> dayMetricsMap,OverAllMetric overAllMetric,String keystoreLn,float costPercent){
        //            Map<String,DayMetric> dayMetricsMap = metricInfo.dayMetricsMap;
        float drawDownPercent=0,cumulativeNetProfitPercent=0,maxCumulativeProfitPercent=-Float.MAX_VALUE,
                maxDrawDownPercentage=-Float.MAX_VALUE;
        float netProfitPercent = 0;
        //for(DayMetric dayMetric:dayMetricsMap.values()) {
        for(Map.Entry<String,DayMetric> entry:dayMetricsMap.entrySet()) {
            String date = entry.getKey();
            DayMetric dayMetric = entry.getValue();
            if(printDayWise)dayMetric.print();
            cumulativeNetProfitPercent += dayMetric.profitPercentWithCost.profit;
            maxCumulativeProfitPercent = Float.max(maxCumulativeProfitPercent,cumulativeNetProfitPercent);
            drawDownPercent = maxCumulativeProfitPercent - cumulativeNetProfitPercent;
            maxDrawDownPercentage = Float.max(maxDrawDownPercentage,drawDownPercent);
            netProfitPercent += dayMetric.profitPercentWithCost.profit;
        }
        overAllMetric.calmar = netProfitPercent/maxDrawDownPercentage;
        overAllMetric.update(new ArrayList<>(dayMetricsMap.values()));
        overAllMetric.calculateOverAllMetricsAndPrint(keystoreLn,costPercent);
    }

    public void calculateOverAll(){
        overAllMetricInfoMap.values().forEach(metricInfo->{
            metricInfo.stockDayMetricsMap.entrySet().forEach(entry->{
                String symbol = entry.getKey();

                Map<String,DayMetric> dayMetricsMap = entry.getValue();
                processDayMetricMap(false,dayMetricsMap,metricInfo.stockOverAllMetric.get(symbol),metricInfo.keystoreLn,metricInfo.costPercent);
            });
        });

        overAllMetricInfoMap.values().forEach(metricInfo->{
            processDayMetricMap(true,metricInfo.dayMetricsMap,metricInfo.overAllMetric,metricInfo.keystoreLn,metricInfo.costPercent);
        });
    }

}