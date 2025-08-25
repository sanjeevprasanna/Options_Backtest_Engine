package com.valar.basestrategy.inputs;

import java.util.LinkedList;

import static com.valar.basestrategy.utils.ValarUtils.getInMinutes;

public class MinimaProfitRange {
    public int checkMinimaAfter,checkMinimaBefore,duration;
    public float minimaProfitExit,maximaProfitExit,maxMinDiff;
    public int minimaWait;

    LinkedList<Float> profitsInPeriods = new LinkedList();

    public int id,count=0,minimaCount;boolean filled = false;

    public MinimaProfitRange(String remainingAttribs){
        int iter = 1;

        String[] splits = remainingAttribs.split(",");

        id = Integer.parseInt(splits[iter++]);

        duration = Integer.parseInt(splits[iter++]);

        String fromCheckTime = splits[iter++];
        checkMinimaAfter = getInMinutes(Integer.parseInt(fromCheckTime.split(":")[0])
                ,Integer.parseInt(fromCheckTime.split(":")[1]));

        String toCheckTime = splits[iter++];
        checkMinimaBefore = getInMinutes(Integer.parseInt(toCheckTime.split(":")[0])
                ,Integer.parseInt(toCheckTime.split(":")[1]));

        minimaProfitExit = Float.parseFloat(splits[iter++]);

        maximaProfitExit = Float.parseFloat(splits[iter++]);

        maxMinDiff = Float.parseFloat(splits[iter++]);

        minimaWait = Integer.parseInt(splits[iter++]);

//        if(pOrA=='a') System.out.println(this);
    }

    public boolean isInRangeAndExitReached(int mins,float totalProfit){

        boolean res = false;
        if(!filled){
            profitsInPeriods.add(totalProfit);
            count++;
            if(count>=duration)
                filled = true;
        }else{
            res =  checkCondition(mins,totalProfit);
            if (!res) {
                profitsInPeriods.remove(0);
                profitsInPeriods.add(totalProfit);
            }
        }

        return res;
    }

    private boolean checkCondition(int mins, float totalProfit){
        float minProfit = Float.MAX_VALUE,maxProfit=-Float.MAX_VALUE;
            for(float pros:profitsInPeriods) {
                if (minProfit > pros)
                    minProfit = pros;
                if(maxProfit < pros)
                    maxProfit = pros;
            }


//        System.out.println("TotalProfit "+totalProfit
//                +" minimaProfitExit "+(totalProfit>=minimaProfitExit)+
//                " maximaProfitExit "+(totalProfit<=maximaProfitExit)+
//                " minProfit "+(totalProfit<minProfit)+
//                " checkMinimaAfter "+(mins>=checkMinimaAfter)+
//                " checkMinimaBefore "+(mins<=checkMinimaBefore)+
//                " maxMinDiff "+((maxProfit - minProfit) >= maxMinDiff)+
//                " maxProfit "+maxProfit+
//                " minProfit "+minProfit+
//                "\n profitsInPeriods "+profitsInPeriods
//                );
            if(totalProfit>=minimaProfitExit && totalProfit<=maximaProfitExit && totalProfit<minProfit && mins>=checkMinimaAfter && mins<=checkMinimaBefore && (maxProfit - minProfit) >= maxMinDiff){
                if(minimaCount==minimaWait) {
                    return true;
                }
                minimaCount++;
            }else minimaCount = 0;

        return false;
    }

    public String getLastProfits(){
        return profitsInPeriods.toString().replaceAll(","," ");
    }

    @Override
    public String toString() {
        return "MinimaProfitRange{" +
                ", checkMinimaAfter=" + checkMinimaAfter +
                ", checkMinimaBefore=" + checkMinimaBefore +
                ", duration=" + duration +
                ", minimaProfitExit=" + minimaProfitExit +
                ", maximaProfitExit=" + maximaProfitExit +
                ", maxMinDiff=" + maxMinDiff +
                ", minimaWait=" + minimaWait +
                ", profitsInPeriods=" + profitsInPeriods +
                ", sno=" + id +
                ", count=" + count +
                ", minimaCount=" + minimaCount +
                ", filled=" + filled +
                '}';
    }
}
