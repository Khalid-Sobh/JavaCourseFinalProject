package com.spark;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.mllib.clustering.KMeans;
import org.apache.spark.mllib.clustering.KMeansModel;
import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class JobDAO implements JobDAOInterface{
    /** Summary *************************************************************************/
    //@Override
    public Dataset<Row> readCSV() {
        //omit the log generated by apache spark from the output.
        //Logger.getLogger ("org").setLevel (Level.ERROR);

        // Create Spark Session to create connection to Spark
        final SparkSession sparkSession = SparkSession.builder ().appName ("Wuzzuf Jobs Project").master ("local[4]")
                .getOrCreate ();
        // Get DataFrameReader using SparkSession
        final DataFrameReader dataFrameReader = sparkSession.read ();

        // Set header option to true to specify that first row in file contains name of columns
        dataFrameReader.option ("header", "true");

        Dataset<Row> jobsDF = dataFrameReader.csv ("src/main/resources/Wuzzuf_Jobs.csv");

        /** Print Schema to see column names, types and other metadata */
        //System.out.println("======================== DataSet Schema ========================");
        /**jobsDF.printSchema ();**/
        //String printSchema = jobsDF.schema ().toString();


        /** Print Summary to see column statistics */
        //System.out.println("======================== DataSet Summary ========================");
        /**CHANGE*/
        /**jobsDF.describe();*/
        //String describe = jobsDF.describe().toString();

        // Print First 20 rows of DataSet
        //System.out.println("======================== First 20 rows of DataSet ========================");
        /**jobsDF.show(20);**/
        //String firstTwenty  = jobsDF.take(20).toString();

        return jobsDF;
    }

    public   String viewDescribe (Dataset<Row> jobsDF){
        // Create a stream to hold the output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        // IMPORTANT: Save the old System.out!
        PrintStream old = System.out;

        // Tell Java to use your special stream
        System.setOut(ps);
        Logger.getLogger ("").setLevel (Level.ERROR);

        // Print some output: goes to your special stream
        System.out.println("================================ DataSet Schema ================================");
        jobsDF.printSchema ();
        System.out.println("\n");
        //jobsDF.describe().show(); //Can be removed
        System.out.println("\n");
        System.out.println("=========================== First 25 rows of DataSet ===========================");
        //jobsDF.show(20); //Can be removed
        // Put things back
        System.out.flush();
        System.setOut(old);
        // Show what happened
        //String describe = baos.toString();
        return "\n" + baos.toString() + "\n" +  jobsDF.showString(25,75,false)
                + "\n" +         "=========================== DataSet Summary ===========================" + "\n" +
        jobsDF.describe().showString(5 , 50 , false);

    }
    /** Cleaning *************************************************************************/

    //@Override
    public Dataset<Row> cleanDataFrame(Dataset<Row> data) {

        Dataset<Row> jobsNoNullDF=data.na().drop ();
        Dataset<Row> cleanDF = jobsNoNullDF.distinct();
        // Print Summary to see column statistics
        //System.out.println("======================== Clean DataSet Summary ========================");
        //cleanDF.describe().show();
        return cleanDF;
    }

    public   String viewClean (Dataset<Row> jobsDF){

        return "\n" +jobsDF.describe().showString(5 , 50 , false);
    }
    /** demandingCompanies *************************************************************************/

    //@Override
    public Dataset<Row> demandingCompanies(Dataset<Row> jobsDF) {
        // Create view and execute query to convert types as, by default, all columns have string types
        jobsDF.createOrReplaceTempView ("Wuzzuf_DF");
        // Create Spark Session to create connection to Spark
        final SparkSession sparkSession = SparkSession.builder ().appName ("Wuzzuf Jobs Project").master ("local[4]")
                .getOrCreate ();
        final Dataset<Row> demandingCompany = sparkSession
                .sql ("SELECT Company,Count(*) AS Available_Jobs FROM Wuzzuf_DF GROUP BY Company ORDER BY Count(*) DESC ");
        /**demandingCompany.show();**/
        return demandingCompany;
    }

    public   String viewDemandingCompanies (Dataset<Row> jobsDF){

        return "\n" + jobsDF.showString(25,75,false);
    }

    public Map<String, Long> mapDemandingCompanies(Dataset<Row> companies){
        //create map of DataPreparation.Job Company & it's count in the file
        Map<String, Long> jobCompany = new HashMap<>();
        companies.collectAsList().forEach(row -> jobCompany.put(row.getString(0), row.getLong(1)));
        //jobCompany.entrySet().forEach(System.out::println);

        //sorted the map Descending
        Map<String, Long> sortedJobCompany = jobCompany.entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));

        /*System.out.println("==================== sortedJobCompany =========================\n");
        sortedJobCompany.entrySet().forEach(System.out::println);*/
        //System.out.println("===============================================================================================\n");

        Map<String, Long> demandingCompanies  = sortedJobCompany.entrySet()
                .stream()
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
        return demandingCompanies;
    }
    /** popularJobTitle *************************************************************************/

    //@Override
    public Dataset<Row> popularJobTitle(Dataset<Row> jobsDF) {
        // Create view and execute query to convert types as, by default, all columns have string types
        jobsDF.createOrReplaceTempView ("Wuzzuf_DF");
        // Create Spark Session to create connection to Spark
        final SparkSession sparkSession = SparkSession.builder ().appName ("Wuzzuf Jobs Project").master ("local[4]")
                .getOrCreate ();
        final Dataset<Row> demandingJobTitle = sparkSession
                .sql ("SELECT Title,Count(*) AS Needed_Jobs FROM Wuzzuf_DF GROUP BY Title ORDER BY Count(*) DESC ");
        /**demandingJobTitle.show();*/
        return demandingJobTitle;
    }
    public   String viewPopularJobTitle (Dataset<Row> jobsDF){

        return "\n" + jobsDF.showString(25,75,false);
    }
    public Map<String, Long> mapPopularJobTitle(Dataset<Row> titles) {
        //create map of DataPreparation.Job Title & it's count in the file
        Map<String, Long> jobTitle = new HashMap<>();
        titles.collectAsList().forEach(row -> jobTitle.put(row.getString(0), row.getLong(1)));
        //jobTitle.entrySet().forEach(System.out::println);

        //sorted the map Descending
        Map<String, Long> sortedJobTitles = jobTitle.entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));

        /*System.out.println("==================== sortedJobTitles  =========================\n");
        sortedJobTitles .entrySet().forEach(System.out::println);
        System.out.println("===============================================================================================\n");*/

        Map<String, Long> demandingTitles  = sortedJobTitles .entrySet()
                .stream()
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
        return demandingTitles;
    }
    /** popularAreas *************************************************************************/

    //@Override
    public  Dataset<Row> popularAreas(Dataset<Row> jobsDF) {
        // Create view and execute query to convert types as, by default, all columns have string types
        jobsDF.createOrReplaceTempView ("Wuzzuf_DF");
        // Create Spark Session to create connection to Spark
        final SparkSession sparkSession = SparkSession.builder ().appName ("Wuzzuf Jobs Project").master ("local[4]")
                .getOrCreate ();
        final Dataset<Row> demandingAreas = sparkSession
                .sql ("SELECT Location,Count(*) AS Counts FROM Wuzzuf_DF GROUP BY Location ORDER BY Count(*) DESC ");
        /**demandingAreas.show();*/
        return demandingAreas;
    }
    public   String viewPopularAreas (Dataset<Row> jobsDF){

        return "\n" + jobsDF.showString(25,75,false);
    }

    public Map<String, Long> mapPopularAreas(Dataset<Row> areas) {
        //create map of DataPreparation.Job Areas & it's count in the file
        Map<String, Long> jobAreas = new HashMap<>();
        areas.collectAsList().forEach(row -> jobAreas.put(row.getString(0), row.getLong(1)));
        //jobTitle.entrySet().forEach(System.out::println);

        //sorted the map Descending
        Map<String, Long> sortedJobAreas= jobAreas.entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));

      /*  System.out.println("==================== sortedJobAreas =========================\n");
        sortedJobAreas .entrySet().forEach(System.out::println);
        System.out.println("===============================================================================================\n");*/

        Map<String, Long> demandingAreas  = sortedJobAreas.entrySet()
                .stream()
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
        return demandingAreas ;
    }



    /** importantSkills *************************************************************************/


        public  Dataset<Row>  importantSkills(Dataset<Row> jobsDF) {
            // Create view and execute query to convert types as, by default, all columns have string types
            jobsDF.createOrReplaceTempView ("Wuzzuf_DF");
            // Create Spark Session to create connection to Spark
            final SparkSession sparkSession = SparkSession.builder ().appName ("Wuzzuf Jobs Project").master ("local[4]")
                    .getOrCreate ();
            final Dataset<Row> Skills = sparkSession.sql ("SELECT Skills  FROM Wuzzuf_DF ");
            return Skills;
            //Skills.show();

        }
    public   String viewImportantSkills (Map<String, Long> sortedJobSkills){
            List<String> list= new ArrayList<>();
        sortedJobSkills.forEach((k,v)-> list.add("  "+k.toUpperCase()+" was found "+v+" times."));
        String str = "";
        int i = 1;
        for (String var : list)
        {
            str = str + "\n" + i + ". " + var;
            i = i + 1 ;
        }

        return str;
    }
    public Map<String, Long> mapImportantSkills(Dataset<Row> Skills) {
        //create map of DataPreparation.Job Areas & it's count in the file
        Map<String, Long> jobSkills = Skills.collectAsList().stream()
                .map(row -> row.getString(0).split(","))
                .flatMap(Arrays::stream)
                .map(skill -> skill.toLowerCase().trim())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        //importantSkills.entrySet().forEach(System.out::println);
        //sorted the map Descending
        Map<String, Long> sortedJobSkills= jobSkills.entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));

        /*System.out.println("==================== sortedJobSkills =========================\n");
        sortedJobSkills.entrySet().forEach(System.out::println);
        System.out.println("===============================================================================================\n");*/

        Map<String, Long> demandingSkills  = sortedJobSkills.entrySet()
                .stream()
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
        return demandingSkills  ;
    }





    /** THE GRAPHS  ************************************************************************/
    //@Override
    public void graphPieChart(Map<String, Long> jobMap,String title) {
        // Create Chart
        PieChart chart = new PieChartBuilder().width (800).height (600).title (title).build ();
        // Customize Chart
        int size =jobMap.size();
        Color[] sliceColors = new Color[size];
        for (int i=0; i<size;i++){
            sliceColors[i] = Color.getHSBColor((float) (i+1) / size, 1, 4);
        }
        chart.getStyler ().setSeriesColors (sliceColors);
        // Series
        for (Map.Entry<String, Long> entry : jobMap.entrySet()) {
            chart.addSeries (entry.getKey(), entry.getValue());
        }
        // Show it
        /**new SwingWrapper(chart).displayChart ();*/
        String picPath = "src/main/webapp/" + title;

        try {
            BitmapEncoder.saveBitmapWithDPI(chart, picPath, BitmapEncoder.BitmapFormat.JPG, 80);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //@Override
    public void graphBarChart(Map<String, Long> jobMap,String title,String xLable,String yLable) {
        //filter to get an array of passenger ages
        List<String> keys = jobMap.keySet().stream().collect(toList());
        List<Long> values = jobMap.values().stream().collect(toList());

        // Create Chart
        CategoryChart chart = new CategoryChartBuilder().width (1024).height (768).title (title).xAxisTitle (xLable).yAxisTitle (yLable).build ();
        // Customize Chart
        chart.getStyler ().setLegendPosition (Styler.LegendPosition.InsideNW);
        chart.getStyler ().setHasAnnotations (true);
        chart.getStyler ().setStacked (true);
        chart.getStyler().setXAxisLabelRotation(90);
        // Series
        chart.addSeries (yLable, keys, values);
        // Show it
        /**new SwingWrapper(chart).displayChart ();*/
        String picPath = "src/main/webapp/" + title;
        try {
            BitmapEncoder.saveBitmapWithDPI(chart, picPath, BitmapEncoder.BitmapFormat.JPG, 80);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /** Factorize *************************************************************************/

    public Dataset<Row> factorizeCol(Dataset<Row> data) {
        Dataset<Row> newDf = new StringIndexer().setInputCol("YearsExp")
                .setOutputCol("YearsExpEncoded")
                .fit(data)
                .transform(data);
        return newDf;
    }
    /***use viewDiscribe to show results */

    /** K-Means **************************************************************************/

    //KMeansModel clusters = KMeans.train();




}

