package com.valar.basestrategy.application;

import static com.valar.basestrategy.application.PropertiesReader.properties;
import static com.valar.basestrategy.utils.KeyValues.setIndexAttribs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.valar.basestrategy.service.Strategy;
import com.valar.basestrategy.utils.ValarUtils;
import com.valar.basestrategy.utils.KeyValues;
import com.valar.basestrategy.utils.PrintWriters;
import com.valar.basestrategy.utils.S3FolderDownloader;

public class ValarTrade {
    public static String keystoreHeading = "";
    public static PrintWriter overAllDayWise,overAllOrderInfo,overAllDetails;
    public static Map<String,String> vixMap=new HashMap<String,String>();
    public static Map<String, String> allFilesMap  = new HashMap<String,String>() {
        {
            put("keystore", properties.getProperty("keystoreFile"));
        }};
    private static List<KeyValues> keyStoresList = new ArrayList<>();

    //Testing comment


    public static void main(String[] args)throws Exception{
        String time1 = LocalTime.now().toString();


        boolean aws =Boolean.parseBoolean(properties.getProperty("aws"));
        if(aws){
            S3FolderDownloader.downloadFolder(true);
            S3FolderDownloader.downloadFolder(false);
        }


        BufferedReader keyValuesReader = new BufferedReader(new FileReader(properties.getProperty("keystoreFile")));
        Set<Integer> kvSerialNos = new HashSet<>();
        ValarTrade valarTrade = new ValarTrade();
        keystoreHeading = keyValuesReader.readLine();
        PrintWriters.loadAllWriters();


        valarTrade.createDirectories(kvSerialNos);
        for(String inputFile : valarTrade.allFilesMap.values())
            copyFile(inputFile,new File("."),new File("./Outputs/"));


        List<String> keystoreLines = Files.readAllLines(Paths.get(allFilesMap.get("keystore")));
        Set<Integer> candlePeriodSet = new HashSet<>();
        for(int i = 1;i < keystoreLines.size();i++){
            KeyValues kv = new KeyValues(keystoreLines.get(i));
            keyStoresList.add(kv);
            candlePeriodSet.add(kv.candlePeriod);
        }


        int[] indexTypes = {0,1};
        boolean[] positional = {true,false};

        for(int indexType : indexTypes) {
            for(boolean pos:positional) {
                for(int candlePeriod : candlePeriodSet) {
                    ValarUtils.indexType = indexType;
                    setIndexAttribs(indexType);
                    applyStrategyOnKeystore(pos,candlePeriod, indexType, keyStoresList.size());
                }
            }
        }

        PrintWriters.closeAllWriters();

        String time2 = LocalTime.now().toString();
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        Date date1 = format.parse(time1);
        Date date2 = format.parse(time2);
        long difference = date2.getTime() - date1.getTime();
        difference=difference/1000;
        System.out.println("Total Time Taken(In Seconds) -> "+difference);
    }

    private static void applyStrategyOnKeystore(boolean positional,int candlePeriod,int bnOrN,int ksSize)throws Exception{
        int runKeystores = Integer.parseInt(properties.getProperty("runKeystores"));
        for(int i = 0;i < ksSize;i++){
            List<KeyValues> runForKeyAttribs = new ArrayList<>();
            int j = i;
            for(; runForKeyAttribs.size() < runKeystores && j < ksSize;j++) {
                KeyValues kv = keyStoresList.get(j);
                if(kv.indexType ==bnOrN && kv.positional==positional && kv.candlePeriod ==candlePeriod) {
                    System.out.println(kv.sno);
                    runForKeyAttribs.add(kv);
                }
            }
            i = j-1;

            if(runForKeyAttribs.size()!=0) {
                Strategy strategy = new Strategy(positional,candlePeriod,runForKeyAttribs);
                strategy.apply();
                strategy.calculateOverAll();
            }
        }
    }

    public void createDirectories(Set<Integer> kvSerialNos){
        String[] dirs = {"./Outputs","./Outputs/serialwise"};
        for(String dir:dirs){
            File outputsDir = new File(dir);
            outputsDir.mkdir();
        }

        for(int sno:kvSerialNos){
            File outputsDir = new File("./Outputs/serialwise/"+sno);
            outputsDir.mkdir();
        }
    }



    public static void copyFile(String fileName,File source, File dest) throws IOException {
        File file = new File(dest+"/"+fileName);
        if(file.exists())file.delete();
        Files.copy(new File(source+"/"+fileName).toPath(),new File(dest+"/"+fileName).toPath());
    }
}

//to run
//mvn clean package dependency:copy-dependencies
// java -cp "$(pwd)/target/BaseStrategy_20250528-1.0-SNAPSHOT.jar:$(pwd)/target/dependency/*" com.valar.basestrategy.application.ValarTrade