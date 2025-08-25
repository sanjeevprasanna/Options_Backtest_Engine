package com.valar.basestrategy.state.minute;

import com.valar.basestrategy.entities.Ohlc;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.num.DecimalNum;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class State {
    public List<String> lines;
    public int parser;
    public String name,line;
    public Ohlc ohlc = new Ohlc(),nextOhlc = new Ohlc();
    public boolean finished;
    private String dateTimeFormat;
    private float gapPercent;
    public BarSeries series;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yy");
    private Map<Integer, EMAIndicator> emaIndicatorMap = new HashMap<Integer,EMAIndicator>();
    private Map<Integer, RSIIndicator> rsiIndicatorMap = new HashMap<Integer,RSIIndicator>();
    //for pivots
    public Float pp, r1, r2, s1, s2;
    public boolean pivotsInitialized ;

    public State(String name, String path, int parser, String dateTimeFormat,boolean removeDayIfDataNotPresent){
        this.name = name;
        this.parser = parser;
        loadLines(path,removeDayIfDataNotPresent);
//        System.out.println(name);
        line = lines.get(parser);
        ohlc.update(line);
        nextOhlc.update(line);
        this.dateTimeFormat = dateTimeFormat;
        finished = parser >= lines.size();
    }

    public State(String name, String path, int parser, String dateTimeFormat,int period,boolean removeDayIfDataNotPresent){
        this.name = name;
        this.parser = parser;
        this.lines = null;
        loadLines(path,removeDayIfDataNotPresent);
        updateLinesAccToPeriod(period);
        line = lines.get(parser);
        ohlc.update(line);
        nextOhlc.update(line);
        this.dateTimeFormat = dateTimeFormat;
        finished = parser >= lines.size();
    }

    public State(String name, String path, String readTill, String dateTimeFormat,int period,boolean removeDayIfDataNotPresent){
        this.name = name;
        loadLines(path,removeDayIfDataNotPresent);
        updateLinesAccToPeriod(period);
        for(parser =0;parser<lines.size();parser++) {
            line = lines.get(parser);
            if(line.startsWith(readTill)) break;
        }
        ohlc.update(line);
        nextOhlc.update(line);
        this.dateTimeFormat = dateTimeFormat;
        finished = parser >= lines.size();
    }

    public State(String name, String path,boolean readB4,String readTillOrB4Dnt, String dateTimeFormat,boolean removeDayIfDataNotPresent){
        this.name = name;
        loadLines(path,removeDayIfDataNotPresent);
        String nextLn = null;
        for(parser =0;parser<lines.size();parser++) {
            line = lines.get(parser);
            if((parser+1)<lines.size())nextLn = lines.get(parser+1);
            if(!readB4 && line.startsWith(readTillOrB4Dnt)) break;
            else if(readB4 && nextLn!=null && nextLn.startsWith(readTillOrB4Dnt))break;
        }
        ohlc.update(line);
        nextOhlc.update(line);
        this.dateTimeFormat = dateTimeFormat;
    }

    private void loadLines(String path,boolean removeDayIfDataNotPresent){
        try{ this.lines = Files.readAllLines(Paths.get(path)); }catch (Exception e){e.printStackTrace();}
        if(removeDayIfDataNotPresent)removeLinesIfEntireDayDataNotPresent();
    }

    private void removeLinesIfEntireDayDataNotPresent(){
        List<String> updatedLines = new ArrayList<>();
        List<String> dayData = new ArrayList<>();
        String[] splits;boolean nonZeroFound = false;
        String date;
        float price;
        String ln,nextLn;
        for(int i = 0;i < lines.size();i++){
            ln = lines.get(i);
            if(i+1 < lines.size())nextLn = lines.get(i+1);
            else nextLn = "";
            splits = ln.split(",");
            date = splits[0].split(" ")[0];
            price = Float.parseFloat(splits[4]);
            if(!nonZeroFound && price!=0)nonZeroFound = true;
            dayData.add(ln);

            if(nextLn.isEmpty() || !date.equals(new Ohlc(nextLn).date)){
                if(nonZeroFound) updatedLines.addAll(dayData);
                dayData.clear();
                nonZeroFound = false;
            }
        }

        lines = updatedLines;
    }


    private void updateLinesAccToPeriod(int period) {
        float open = 0, high = -Float.MAX_VALUE, low = Float.MAX_VALUE, close = 0, volume = 0;
        String startDnt = null, endDnt = null;
        int count = 0;
        List<String> updatedLines = new ArrayList<>();
        String[] splits;

        for (int i = 0; i < lines.size(); i++) {
            String currentLine = lines.get(i);
            splits = currentLine.split(",");

            String currentDnt = splits[0];
            String currentDate = currentDnt.split(" ")[0];

            String nextDnt = (i + 1 < lines.size()) ? lines.get(i + 1).split(",")[0] : null;
            String nextDate = (nextDnt != null) ? nextDnt.split(" ")[0] : null;

            float o = Float.parseFloat(splits[1]),
                    h = Float.parseFloat(splits[2]),
                    l = Float.parseFloat(splits[3]),
                    c = Float.parseFloat(splits[4]),
                    v = splits.length >= 6 ? Float.parseFloat(splits[5]) : 0;

            if (count == 0) {
                startDnt = currentDnt;
                open = o;
                high = h;
                low = l;
                close = c;
                volume = v;
            } else {
                high = Math.max(high, h);
                low = Math.min(low, l);
                close = c;
                volume += v;
            }

            count++;

            boolean isEndOfPeriod = count == period;
            boolean isEndOfDay = (nextDate != null && !currentDate.equals(nextDate));
            boolean isLastLine = (i == lines.size() - 1);

            if (isEndOfPeriod || isEndOfDay || isLastLine) {
                if (isLastLine || isEndOfDay) {
                    endDnt = currentDate + " 15:30";
                } else {
                    endDnt = nextDnt;
                }

                updatedLines.add(startDnt + "," + open + "," + high + "," + low + "," + close + "," + volume + "," + endDnt);
                count = 0;
            }
        }

        lines = updatedLines;
    }

    private List<String> getUpdateLinesAccToPeriod(int period){
        if(period<=1)return lines;
        List<String> lines = new ArrayList(this.lines);
        float open=0,high = -Float.MAX_VALUE,
                low=Float.MAX_VALUE,close = 0,volume=0;
        String dnt,lastSavedDnt=null;
        int periodCounter = 0;
        String[] splits;
        List<String> updatedLines = new ArrayList<>();
        for(String ln : lines){
            splits = ln.split(",");
            dnt = splits[0];
            float o = Float.parseFloat(splits[1]),
                    h = Float.parseFloat(splits[2]),
                    l = Float.parseFloat(splits[3]),
                    c = Float.parseFloat(splits[4]),v = 0;
            if(splits.length>=6) v = Float.parseFloat(splits[5]);

            if((periodCounter)%period==0 || dnt.contains("09:15")){
                if(lastSavedDnt!=null)
                    updatedLines.add(lastSavedDnt + "," + open + "," + high
                            + "," + low + "," + close + "," + volume);
                lastSavedDnt = dnt;
                open = o;
                high = h;
                low = l;
                close = c;
                volume = v;
                periodCounter = 0;
            }else{
                high = Float.max(high,h);
                low = Float.min(low,l);
                close = c;
                volume+=v;
            }
            periodCounter++;
        }

        updatedLines.add(lastSavedDnt + "," + open + "," + high
                + "," + low + "," + close + "," + volume);

        return updatedLines;
    }

    public BarSeries loadSeries(){
        List<String> lines;
        lines = this.lines;
//        else lines = getUpdateLinesAccToPeriod(period);
        DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(dateTimeFormat);
        BarSeries series = new BaseBarSeries(name);
        String ln,dnt;
        int i=0;
        for(;i<lines.size();i++) {
            ln = lines.get(i);
            String[] lineSplits = ln.split(",");
            dnt = lineSplits[0];
            ZonedDateTime date = LocalDateTime.parse(dnt, DATE_TIME_FORMATTER).atZone(ZoneId.systemDefault());
            double openPrice = Double.parseDouble(lineSplits[1]);
            double highPrice = Double.parseDouble(lineSplits[2]);
            double lowPrice = Double.parseDouble(lineSplits[3]);
            double closePrice = Double.parseDouble(lineSplits[4]);
            double volume = 0;

            if (highPrice==lowPrice) {
                // Introduce a minimal variation (optional for synthetic data)
                highPrice +=0.01;  // Small increment to avoid NaN
                lowPrice -= 0.01;
            }


//            if(closePrice==0 || highPrice==0 || lowPrice==0 || openPrice==0) continue;
            try {volume = Double.parseDouble(lineSplits[5]);} catch (Exception e) {}

            BaseBar bar = BaseBar.builder(DecimalNum::valueOf, Number.class)
                    .timePeriod(Duration.ofMinutes(1))
                    .endTime(date)
                    .openPrice(openPrice)
                    .highPrice(highPrice)
                    .lowPrice(lowPrice)
                    .closePrice(closePrice)
                    .volume(volume)
                    .build();
            series.addBar(bar);
        }
        return series;
    }

    public void loadIndicatorsFromPreviousLines(int readFrom){
        for(int i = Integer.max(0,parser-readFrom);i<=parser;i++){
            line = lines.get(i);
            ohlc.update(line);
            updateIndicators();
        }
        if(parser+1<lines.size())nextOhlc.update(lines.get(parser+1));
    }

    public void readTillDateWhileUpdatingIndicatorsFromLinesB4(String readTill,int updateIndicatorsFrom){
        for(parser = 0;parser<lines.size();parser++) {
            line = lines.get(parser);
            if(line.startsWith(readTill)) break;
        }

        if(parser!=lines.size())
            for (int i = Integer.max(0, parser - updateIndicatorsFrom); i <= parser; i++) {
                line = lines.get(i);
                ohlc.update(line);
                updateIndicators();
            }
        if(parser+1<lines.size())nextOhlc.update(lines.get(parser+1));
    }

    public void readTillDate(boolean startFromBeginning,boolean readB4,String readTillOrB4Dnt){
        if(startFromBeginning)parser=0;
        String nextLn = null;
        LocalDate readTillDate = LocalDate.parse(readTillOrB4Dnt.split(" ")[0], dateFormatter),
                thisDate = LocalDate.parse(ohlc.date, dateFormatter);
        if(thisDate.isAfter(readTillDate))return;
        for(;parser<lines.size();parser++) {
            line = lines.get(parser);
            ohlc.update(line);
            thisDate = LocalDate.parse(ohlc.date, dateFormatter);
            if(thisDate.isAfter(readTillDate))return;
            if((parser+1)<lines.size())nextLn = lines.get(parser+1);
            if(!readB4 && line.startsWith(readTillOrB4Dnt)) break;
            else if(readB4 && nextLn!=null && nextLn.startsWith(readTillOrB4Dnt))break;
        }
        if(nextLn!=null)nextOhlc.update(nextLn);
    }

    public void readTillDateWhileUpdatingIndicators(boolean startFromBeginning,boolean readB4,String readTillOrB4Dnt){
        if(startFromBeginning)parser=0;
        String nextLn = null;
        for(;parser<lines.size();parser++) {
            line = lines.get(parser);
            if((parser+1)<lines.size())nextLn = lines.get(parser+1);
            ohlc.update(line);
            updateIndicators();
            if(!readB4 && line.startsWith(readTillOrB4Dnt)) break;
            else if(readB4 && nextLn!=null && nextLn.startsWith(readTillOrB4Dnt))break;
        }
        if(nextLn!=null)nextOhlc.update(nextLn);
    }

    public void loadIndicators(int emaPeriod,int rsiPeriod) {

        if(series==null) {
            series = loadSeries();
        }
            if(!emaIndicatorMap.containsKey(emaPeriod)) {
                BarSeries series;
                series = this.series;
                ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
                EMAIndicator emaIndicator = new EMAIndicator(closePrice, emaPeriod);
                //EMAEntity emaEntity=new EMAEntity(series,emaPeriod);
                emaIndicatorMap.put(emaPeriod,emaIndicator );
            }

            if(!rsiIndicatorMap.containsKey(rsiPeriod)) {
                BarSeries series;
                series = this.series;
                ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
                RSIIndicator rsiIndicator = new RSIIndicator(closePrice, rsiPeriod);
                rsiIndicatorMap.put(rsiPeriod, rsiIndicator);
            }

    }
    public double getEmaVal(int emaPeriod) {

            return emaIndicatorMap.get(emaPeriod).getValue(parser).doubleValue();
    }
    public double getRsiVal(int rsiPeriod) {
            return  rsiIndicatorMap.get(rsiPeriod).getValue(parser).doubleValue();
    }

 public void computePivots(float high, float low, float close) {
        this.pp = (high + low + close) / 3f;
        this.r1 = 2 * pp - low;
        this.s1 = 2 * pp - high;
        this.r2 = pp + (high - low);
        this.s2 = pp - (high - low);
        this.pivotsInitialized = true;
    }

    public float nearestAbove(float entryPrice, float high10) {
        if (!pivotsInitialized) throw new IllegalStateException("Pivots not initialized");

        if (entryPrice < r1) return r1;
        if (entryPrice < r2) return r2;

        if (high10 > entryPrice) return high10;
        return entryPrice + 0.01f;
    }

    public float nearestBelow(float entryPrice, float low10 ) {
        if (!pivotsInitialized) throw new IllegalStateException("Pivots not initialized");

        if (entryPrice > s1) return s1;
        if (entryPrice > s2) return s2;

        if (low10 < entryPrice) return low10;

        return entryPrice - 0.01f;
    }

    /*public void loadAdxIndicator(List<Object> adxInputs){
        if(series==null) {
            series = loadSeries();
        }
        if(adxInputs!=null) {
            int period = Integer.parseInt(adxInputs.get(0).toString());
            int smoothingPeriod =Integer.parseInt(adxInputs.get(1).toString());
            String key  = period+":"+smoothingPeriod;
            if(!adxIndicatorMap.containsKey(key)) {
                BarSeries series;
                series = this.series;
                ADXEntity adxEntity=new ADXEntity(series,period,smoothingPeriod);
                adxIndicatorMap.put(key, adxEntity);
            }
        }
    }*/


    public void updateIndicators() {

    }

    public String getNext(boolean updateIndicators){
        parser++;
        finished = parser >= lines.size();
        if(!finished){
            line = lines.get(parser);
            ohlc.update(line);
            if(parser+1<lines.size())nextOhlc.update(lines.get(parser+1));
            if(ohlc.time.equalsIgnoreCase("09:16")) gapPercent = Math.abs(ohlc.open - ohlc.lastMinClose)/ohlc.lastMinClose * 100;
            if(updateIndicators)
                updateIndicators();
            return line;
        }else return null;
    }

    public float getGapPercent(){
        return gapPercent;
    }

    public void updateLineIndex(int parser){
        this.parser = parser;
        line = lines.get(parser);
        ohlc.update(line);
    }



    public void loadVixSeriesAndMap() {
        System.out.println("Vix Loading...");
        if (series == null) {
            series = loadSeries();
        }
        String lastClosePrice = "";
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm");

        for (int i = 0; i < series.getBarCount(); i++) {
            String currentDate = series.getBar(i).getSimpleDateName();
            try {
                LocalDateTime dateTime = LocalDateTime.parse(currentDate, inputFormatter);
                String formattedDate = dateTime.format(outputFormatter);
                String currentClose = series.getBar(i).getClosePrice().doubleValue() + ",";
                if (dateTime.getHour() == 15 && dateTime.getMinute() == 29) {
                    lastClosePrice = String.valueOf(series.getBar(i).getClosePrice().doubleValue());
                }

//                ValarTrade.vixMap.put(formattedDate, currentClose + lastClosePrice);

            } catch (DateTimeParseException e) {
                System.err.println("Error parsing date: " + currentDate);
            }
        }
        System.out.println("Vix Loaded......");


    }

}
