package com.valar.basestrategy.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.*;

public class ValarUtils {

    public static int indexType;
    public static String indexFile,indexDayFile, tradingDatesFile,expiryDatesFile;
    public static int strikePlus;
    public static String optionsDataPath;
    public static int dataAvailableFromYr;
    public static List<String> indexLines;
    public static List<String> listOfFiles;

    public static boolean printOrderInfoAndSerialWise = true;

    public static Map<String,String> dayAtrMap = new HashMap<String,String>(),minAtrMap = new HashMap<String,String>();
    public static Map<String,String> dayEMAMap = new HashMap<String,String>(),minEMAMap = new HashMap<String,String>();
    public static Map<String,Float> lastDayCloseMap = new HashMap<String,Float>();

    public static void addOrRemove(List<Double> list, int period, double add){
        if(list.size()>=period){
            list.remove(0);
        }list.add(add);
    }

    public static int getInMinutes(String time){
        String[] timeSplits = time.split(":");
        return (Integer.parseInt(timeSplits[0]) * 60) + Integer.parseInt(timeSplits[1]);
    }

    public static int getInMinutes(int hr,int min){
        return (hr * 60) + min;
    }

    public static List<String> getAllFilesOfFolder(String path) {

        File file = new File(path);
        String[] directories = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isFile();
            }
        });

        return Arrays.asList(directories);
    }

    public static String yrMonthDate(String date){
        String day = date.split("-")[0],month = date.split("-")[1],yr = date.split("-")[2];
        return "20"+yr+"-"+month+"-"+day;
    }

    public static ArrayList[] getFiles(String s){
        ArrayList<String> filesCE = new ArrayList(),filesPE = new ArrayList();
        for (String file:listOfFiles) {
            if(file.startsWith(s)) {
                if(file.contains("PE"))
                    filesPE.add(file);
                else
                    filesCE.add(file);
            }
        }
        return new ArrayList[]{filesPE,filesCE};
    }

    public static String getInitPath(String file){
        try {
            int yr;;

            if (indexType == 0) yr = Integer.parseInt(file.substring(14, 16));
            else yr = Integer.parseInt(file.substring(10, 12));

            int diff = yr - dataAvailableFromYr;

            return optionsDataPath + "20" + (dataAvailableFromYr + diff) + "/";
        }catch (Exception e){return "";}
    }

    public static List<String> getKeyValuesAsString(String file,String sn){
        List<String> list = new ArrayList();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            br.readLine();
            String s;int sno = Integer.parseInt(sn);
            List<String> ifNoneFound = new ArrayList<>();
            boolean found = false;
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
//            System.out.println("For "+file+" ks is "+list);

            br.close();

        }catch (Exception e){ e.printStackTrace();System.exit(0); }

        return list;
    }
}
