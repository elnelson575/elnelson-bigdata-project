# Final Project Readme for Elizabeth Nelson (elnelson)

## Project Summary

#### Goals
The goal of this project was to create a web application that could display current COVID safety-related metrics about a user's potential destination.

#### Key Application Links/Locations
<b>Thrift Serialization Jar File: </b>/home/hadoop/elnelson/target/uber-weeklysummarypatterns-1.0-SNAPSHOT.jar<br>
<br>
<b> Deployed Application: </b> http://mpcs53014-loadbalancer-217964685.us-east-2.elb.amazonaws.com:3555/homepage.html <br>
<br>
<b> Spark job: </b> /home/hadoop/elnelson/target/uber-speed_layer_consume2-1.0-SNAPSHOT.jar

#### Instructions to Run the Application
<i> All of this can be observed live in the video provided. Apologies for the bad audio, but the video still demonstrates the below steps in action </i>
1. Navigate to /home/hadoop/elnelson/target
2. Use command <br>
`
spark-submit --master local[2] --driver-java-options "-Dlog4j.configuration=file:///home/hadoop/ss.log4j.properties" --class StreamWeather uber-speed_layer_consume2-1.0-SNAPSHOT.jar b-2.mpcs53014-kafka.fwx2ly.c4.kafka.us-east-2.amazonaws.com:9092,b-1.mpcs53014-kafka.fwx2ly.c4.kafka.us-east-2.amazonaws.com:9092
`
<br>
3. Navigate to web app in browser: <href> http://mpcs53014-loadbalancer-217964685.us-east-2.elb.amazonaws.com:3555/homepage.html </href>
4. Enter address and press submit-- observe ids of Kafka messages being written printed to console.<br>
5. If desired, observe the new appended values in the rows with ids printed to the Kafka console in Hbase.

## Data Sources & Acquisition

* <b> Location Data: </b> Cellphone location data is courtesy of SafeGraph, which is a data provider of anonymized cellphone location data for companies looking to optimize foot traffic, etc. During the COVID pandemic, they have been offering free access to most of their products for research purposes. I had gained access previously for a project for my machine learning class in the spring, so I used my pre-existing credentials to download a subset of their Weekly Patterns dataset. I downloaded three weeks of data, where each row is a 'place of interest' (generally a store or some sort of location people visit) in a particular week and its associated metrics. Additional information available here: <href> https://docs.safegraph.com/docs/weekly-patterns </href><br>

* <b> Mask Wearing Survey Data: </b> Survey data regarding individual's propensity to wear masks when they expect to be within 6 feet of other people was gathered by the New York Times in a single-instance survey.  This data is publicly available here: <href>https://github.com/nytimes/covid-19-data/tree/master/mask-use</href>
<br>
* <b> COVID Cumulative Cases: </b> The cumulative COVID case load by county is collected on an ongoing basis from the New York Times. The base layer dataset was pulled from a recent download from <href> https://github.com/nytimes/covid-19-data/blob/master/us-counties.csv </href><br>
Updates for the speed layer were pulled from the NYT ongoing updated live data (updated several times a day) available here: <href> https://github.com/nytimes/covid-19-data/blob/master/live/us-counties.csv </href>


## Data Ingestion

#### SafeGraph Data:

Pulling data from SafeGraph requires credentials I stored locally awhile ago and promptly forgot how I set it up, so I first downloaded the data locally following the below. Ideally in a production application I think I would set up a better pipeline to go directly to s3, but because I didn't want to deal with wrangling my multiple AWS keys, I chose to download my limited dataset locally and delete it once I finish processing.

##### To download only the last week if this data were to be frequently updated in the application:

```bash
# Save Current month
CURRENTMONTH=$(date +"%m")

# Get names of files available in current month
aws s3 ls s3://sg-c19-response/weekly-patterns-delivery/weekly/patterns/2020/${CURRENTMONTH}/ --profile safegraphws --endpoint https://s3.wasabisys.com > /Users/enelson5/My_Documents/Big_Data_App_Arch/FINAL_PROJECT/available_dirs.txt

# Get last file available in current month
LASTFILE=`tail -n 1 /Users/enelson5/My_Documents/Big_Data_App_Arch/FINAL_PROJECT/available_dirs.txt`

mkdir /Users/enelson5/My_Documents/Big_Data_App_Arch/data_processing_zone/

aws s3 sync s3://sg-c19-response/weekly-patterns-delivery/weekly/patterns/2020/${CURRENTMONTH}/${LASTFILE: -3} /Users/enelson5/My_Documents/Big_Data_App_Arch/data_processing_zone/ --profile safegraphws --endpoint https://s3.wasabisys.com

CURRENTDATE=`date +"%d-%b-%Y"`

tar -C /Users/enelson5/My_Documents/Big_Data_App_Arch/data_processing_zone/${LASTFILE: -3} -cvf ${CURRENTDATE}.tar .

mv ./${CURRENTDATE}.tar /Users/enelson5/My_Documents/Big_Data_App_Arch/FINAL_PROJECT/data_processing/ && rm -R /Users/enelson5/My_Documents/Big_Data_App_Arch/data_processing_zone/${LASTFILE: -3}
```


##### To download additional weeks (first data pull only):

```bash
aws s3 sync s3://sg-c19-response/weekly-patterns-delivery/weekly/patterns/2020/11/04/19/ /Users/enelson5/My_Documents/Big_Data_App_Arch/data_processing_zone/ --profile safegraphws --endpoint https://s3.wasabisys.com

CURRENTDATE="04-11-2020"

tar -C /Users/enelson5/My_Documents/Big_Data_App_Arch/data_processing_zone/ -cvf ${CURRENTDATE}.tar .

mv ./${CURRENTDATE}.tar /Users/enelson5/My_Documents/Big_Data_App_Arch/FINAL_PROJECT/data_processing/ && rm -R /Users/enelson5/My_Documents/Big_Data_App_Arch/data_processing_zone/

aws s3 sync s3://sg-c19-response/weekly-patterns-delivery/weekly/patterns/2020/11/11/18/ /Users/enelson5/My_Documents/Big_Data_App_Arch/data_processing_zone/ --profile safegraphws --endpoint https://s3.wasabisys.com

CURRENTDATE="11-11-2020"

tar -C /Users/enelson5/My_Documents/Big_Data_App_Arch/data_processing_zone/ -cvf ${CURRENTDATE}.tar .

mv ./${CURRENTDATE}.tar /Users/enelson5/My_Documents/Big_Data_App_Arch/FINAL_PROJECT/data_processing/ && rm -R /Users/enelson5/My_Documents/Big_Data_App_Arch/data_processing_zone/
```

##### Thrift Serialization

My SafeGraph data files were by far the largest portion of my project data, so I chose to Thrift serialize them to reduce space. The .jar file is included in the application zip and is also located at `/home/hadoop/elnelson/target/uber-weeklysummarypatterns-1.0-SNAPSHOT.jar` on the cluster.

I ran this using the standard run jar command:
`yarn jar uber-weeklysummarypatterns-1.0-SNAPSHOT.jar edu.uchicago.elnelson.SerializeSG  s3://elnelson-mpcs-project/ /elnelson/sgdata/`

##### Hive Table Creation

###### Create table in Hive linked to sequence file

```SQL
add jar /home/hadoop/elnelson/target/weeklysummarypatterns-1.0-SNAPSHOT.jar;
CREATE EXTERNAL TABLE IF NOT EXISTS elnelson_SG
  ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.thrift.ThriftDeserializer'
    WITH SERDEPROPERTIES (
      'serialization.class' = 'edu.uchicago.elnelson.sgWeekly.weekPOI',
      'serialization.format' =  'org.apache.thrift.protocol.TBinaryProtocol')
  STORED AS SEQUENCEFILE
  LOCATION '/elnelson/sgdata/sgfile-2020-11-28';
```

#### NYT Mask & Historical COVID Data

The NYT data were much smaller, so I decided to put them directly in HDFS as CSVs from my local computer as opposed to serializing. I did this through the following procedure:

##### Move files into HDFS

###### Create the directory
```bash
curl -x "socks5h://localhost:8157" -X PUT -i"http://ec2-52-15-169-10.us-east-2.compute.amazonaws.com:9870/webhdfs/v1/tmp/elnelson/webhdfs?op=MKDIRS&user.name=hadoop"
```

###### Get a destination for the files
```bash
curl -x "socks5h://localhost:8157" -X PUT "http://ec2-52-15-169-10.us-east-2.compute.amazonaws.com:9870/webhdfs/v1/tmp/elnelson/webhdfs/mask-use-by-county.csv?op=CREATE&user.name=hadoop&noredirect=true"
```
```JSON
{"Location":"http://ip-172-31-5-87.us-east-2.compute.internal:9864/webhdfs/v1/tmp/elnelson/webhdfs/mask-use-by-county.csv?op=CREATE&user.name=hadoop&namenoderpcaddress=ip-172-31-11-144.us-east-2.compute.internal:8020&createflag=&createparent=true&overwrite=false"}
 ```
```bash
 curl -x "socks5h://localhost:8157" -X PUT "http://ec2-52-15-169-10.us-east-2.compute.amazonaws.com:9870/webhdfs/v1/tmp/elnelson/webhdfs/nyt-data-update.csv?op=CREATE&user.name=hadoop&noredirect=true"
 ```
 ```JSON
{"Location":"http://ip-172-31-5-87.us-east-2.compute.internal:9864/webhdfs/v1/tmp/elnelson/webhdfs/nyt-data-update.csv?op=CREATE&user.name=hadoop&namenoderpcaddress=ip-172-31-11-144.us-east-2.compute.internal:8020&createflag=&createparent=true&overwrite=false"}
 ```

###### Send the files

 ```bash
cat mask-use-by-county.csv | curl -x "socks5h://localhost:8157" -X PUT -T - "http://ip-172-31-5-87.us-east-2.compute.internal:9864/webhdfs/v1/tmp/elnelson/webhdfs/mask-use-by-county.csv?op=CREATE&user.name=hadoop&namenoderpcaddress=ip-172-31-11-144.us-east-2.compute.internal:8020&createflag=&createparent=true&overwrite=false"
```
```bash
cat nyt-data-update.csv | curl -x "socks5h://localhost:8157" -X PUT -T - "http://ip-172-31-10-67.us-east-2.compute.internal:9864/webhdfs/v1/tmp/elnelson/webhdfs/nyt-data-counties.csv?op=CREATE&user.name=hadoop&namenoderpcaddress=ip-172-31-11-144.us-east-2.compute.internal:8020&createflag=&createparent=true&overwrite=false"
 ```

##### Create Hbase Tables & Hive Tables (Mask Data)
`create 'elnelson_maskdata', 'COUNTYFP'`

###### Transfer the data (Mask Data)
```SQL
CREATE EXTERNAL TABLE raw_mask_use (COUNTYFP int, NEVER double, RARELY double, SOMETIMES double, FREQUENTLY double, ALWAYS double)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES ('hbase.columns.mapping' = ':key,COUNTYFP:NEVER,COUNTYFP:RARELY, COUNTYFP:SOMETIMES,COUNTYFP:FREQUENTLY,COUNTYFP:ALWAYS')
TBLPROPERTIES ('hbase.table.name' = 'elnelson_maskdata');


CREATE EXTERNAL TABLE IF NOT EXISTS temp_masktable
(COUNTYFP int, NEVER double, RARELY double, SOMETIMES double, FREQUENTLY double, ALWAYS double)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION '/tmp/elnelson/webhdfs/';

LOAD DATA INPATH '/tmp/elnelson/webhdfs/mask-use-by-county.csv' INTO TABLE temp_masktable;

insert overwrite table raw_mask_use
SELECT * from temp_masktable
WHERE countyfp > 0;
```

###### Create Hbase table (COVID data)
`create 'elnelson_COVIDhistdata', 'fipsdate'`

###### Create Hive table (COVID data)
```SQL
CREATE EXTERNAL TABLE elnelson_covid_data (fipsdate string, day string, county string, state string, fips int, cases int, deaths int)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES ('hbase.columns.mapping' = ':key,fipsdate:day, fipsdate:county, fipsdate:state, fipsdate:fips, fipsdate:cases, fipsdate:deaths')
TBLPROPERTIES ('hbase.table.name' = 'elnelson_covid_data');

CREATE EXTERNAL TABLE IF NOT EXISTS temp_covid
(day string, county string, state string, fips int, cases int, deaths int)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS TEXTFILE;

LOAD DATA INPATH '/tmp/elnelson/webhdfs/nyt-data-update.csv' INTO TABLE temp_covid;

insert overwrite table elnelson_covid_data
SELECT CONCAT(fips, '-', day) as fipsdate, * from temp_covid
WHERE fips > 0;
```

## View Creation
Because the COVID data were pre-processed in a manner that was useful for the application (with the exception of the creation of unique IDs with the FIPS code) it was not necessary to make additional view tables of them. The Mask data needed minimal processing into a view to deal with the IDs, as seen below. I chose to put it at the FIPS code level because that's what NYT had available, and I knew I could crosswalk the FIPS code into the GEOID.
<br>
The SafeGraph data were in a highly disaggregated form and thus needed to be processed into something that would provide visitor counts on a Census Tract level, as opposed to an individual place of interest-level. I chose Census Tract because it is a reasonably small area (particularly in urban areas where one would expect the concerns of running into individuals to be higher). The alternative geographic areas deducible from GEOID, Census Block Group (a level down) and County (a level up) both seemed less broad than would be ideal for location data.

### Safe Graph Data View Creation
###### Create the table in Hbase
```shell
create 'elnelson_sg_summary2', 'census_tract'
```
###### Process the data in HiveQL
```SQL
create external table elnelson_sg_summary2 (
 census_tract string, avg_Monday double, avg_Tuesday double, avg_Wednesday double, avg_Thursday double, avg_Friday double, avg_Saturday double, avg_Sunday double
  )
  STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
  WITH SERDEPROPERTIES ('hbase.columns.mapping' = ':key,census_tract:avg_Monday,census_tract:avg_Tuesday,census_tract:avg_Wednesday,census_tract:avg_Thursday,census_tract:avg_Friday,census_tract:avg_Saturday,census_tract:avg_Sunday')
  TBLPROPERTIES ('hbase.table.name' = 'elnelson_sg_summary2');

create table elnelson_sg_b as
select split(test,',') as test2, * from (select trim(regexp_replace(visits_by_day, "[^0-9,]", " ")) as test, * from elnelson_sg) as a;

create table elnelson_sg_c as
SELECT cast(test2[0] as int) as Monday, cast(test2[1] as int) as Tuesday, cast(test2[2] as int) as Wednesday, cast(test2[3] as int) as Thursday, cast(test2[4] as int) as Friday, cast(test2[5] as int) as Saturday, cast(test2[6] as int) as Sunday, date_range_start, concat_ws("_",'Tract', substr(string(poi_cbg),1,11)) as census_tract from elnelson_sg_b;

create table elnelson_sg_e as
select census_tract, sum(Monday) as sum_Monday, sum(Tuesday) as sum_Tuesday, sum(Wednesday) as sum_Wednesday, sum(Thursday) as sum_Thursday, sum(Friday) as sum_Friday, sum(Saturday) as sum_Saturday, sum(Sunday) as sum_Sunday
from elnelson_sg_c
group by census_tract, date_range_start;

create table elnelson_sg_g as
select census_tract, avg(sum_Monday) as avg_Monday, avg(sum_Tuesday) as avg_Tuesday, avg(sum_Wednesday) as avg_Wednesday, avg(sum_Thursday) as avg_Thursday, avg(sum_Friday) as avg_Friday, avg(sum_Saturday) as avg_Saturday, avg(sum_Sunday) as avg_Sunday
from elnelson_sg_e
group by census_tract;

create table elnelson_sg_f as
select *, substr(census_tract,7,5) as fips
from elnelson_sg_g;

```
##### Comments on the above process
Of note, the above involves the creation of numerous intermediate tables, which is not optimal. Originally I structured this to involve only temporary table creation using 'with' statements, however this caused the query to become <i>exceptionally</i> slow and take a massive amount of resources, essentially eating the processing power of the cluster. However, when I split the tables into different steps, they ran almost instantaneously. I'm not sure why the performance was quite so intensely different, but I chose to split the job into many queries out of respect for my classmates need to also use the cluster! If I was revising this process, I would likely attempt to see if there is a more efficient way to write this in Scala that could maintain the performance gains of multiple tables while not requiring quite so much intermediate table storage.

#### Mask Data
###### Create the table in Hbase
```bash
create 'elnelson_sg_mask', 'census_tract'
```

###### Process data into table using HiveQL
```SQL

create external table elnelson_sg_mask (
 census_tract string, avg_Monday double, avg_Tuesday double, avg_Wednesday double, avg_Thursday double, avg_Friday double, avg_Saturday double, avg_Sunday double, fips string, countyfp string, never double, rarely double, sometimes double, frequently double, always double
  )
  STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
  WITH SERDEPROPERTIES ('hbase.columns.mapping' = ':key,census_tract:avg_Monday,census_tract:avg_Tuesday,census_tract:avg_Wednesday,census_tract:avg_Thursday,census_tract:avg_Friday,census_tract:avg_Saturday,census_tract:avg_Sunday, census_tract:fips, census_tract:countyfp, census_tract:never, census_tract:rarely, census_tract:sometimes, census_tract:frequently, census_tract:always')
  TBLPROPERTIES ('hbase.table.name' = 'elnelson_sg_mask');

insert overwrite table elnelson_sg_mask
select * from elnelson_sg_f
join raw_mask_use on elnelson_sg_f.fips = raw_mask_use.countyfp;
```

## Speed Layer

My speed layer involves inputs from the NYT COVID cases live updates, which are generally done a few times a day at: <href>https://github.com/nytimes/covid-19-data/blob/master/live/us-counties.csv</href>
<br>
In a production application, I would have an ongoing spark job that would run a few times a day to gather information from this URL, process it, and append it to the COVID historical Hbase table. I would also get the updated historical file with the new counts finalized after each day from the NYT historical data github URL. <br>
For this application, since we were not suppose to have an ongoing spark job running and I wanted to make sure graders could see the spark job actively run (rather than need to wait for one of the 3x daily updates on a delay), I tied the update job to the submit on the address fields. The address fields do not influence the data update itself, but it seemed to be a good trigger to allow the grader to observe the spark job. <br>
One issue I observed was that my producer seems to outstrip my consumer, in that the consumer only 'catches' the first few updates written to the topic. I can see in the topic they are all being correctly written, but the producer is not able to pick them all up. When I asked about this on Piazza, it was suggested this could be a parallelism issue and in a production application, I would use that to fix these (but that we wouldn't be penalized for this). For this reason, I display COVID data up to the first of the month entered by the user, because I knew that this data would be covered by my historical upload. However, ideally I would have data up to the day (as is visible for the Hbase appended rows) and I am set up to collect and display data up to the day with the exception of the Producer/Consumer issue.

#### NYT Real-time COVID data
I created a kafka topic, 'elnelson-covid2' to hold the data updates. <br>

`
kafka-topics.sh --create --zookeeper z-2.mpcs53014-kafka.fwx2ly.c4.kafka.us-east-2.amazonaws.com:2181,z-3.mpcs53014-kafka.fwx2ly.c4.kafka.us-east-2.amazonaws.com:2181,z-1.mpcs53014-kafka.fwx2ly.c4.kafka.us-east-2.amazonaws.com:2181 --replication-factor 1 --partitions 1 --topic elnelson-covid2
`

<br>
The grader is welcome to observe current and past messages that should be stored there. The producer will also print some IDs that it is running to the console as it processes if the grader wishes to observe the appended values in the Hbase table.  There is also a video of me running this process live including showing the updated Hbase values if the grader prefers.

## UI Components

### Geocoding
I do not assume users will know their census tract GEOID offhand (very few people do) so I wanted to make the application able to take an address in a normal form and translate this into a census tract (and from that, a FIPS code) that I could use to filter data. <br>
I tried a few different solutions, but the one that seemed to cooperate the best with my minimal understanding of JS promises was the Census Geocoding API. To use the API, I take in an address in the specified input boxes and format them into a URL that the Census geocoding API can understand. It returns back a JSON object on a webpage, that I translate into text and parse for the GEOID, from which I can parse out the Census Tract. <br>
I included some minor error handling in order to deal with the case in which the geocoder does not recognize the address and does not return back an address match. In this case, the application will send back html with '-' for the missing data. I tested this with a former address of mine, which it turns out is not recognized by the Census geocoder.

### Formatting
I used some relatively limited formatting from Bootstrap to create the navigation bar and the tables of the data. <br>
The icon in the navigation bar comes from here: https://civilresolutionbc.ca/wp-content/uploads/2020/03/covid-19.png
