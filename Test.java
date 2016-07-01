import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.ByteWritable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;
//import org.apache.hadoop.mapreduce.Mapper.Context;
//import org.apache.hadoop.mapreduce.Reducer;
//import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.SerializationHelper;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;

@SuppressWarnings("unused")
public class Test {

	static class OriginMapper extends Mapper<LongWritable, Text, Text, Text> {
		public void map(LongWritable file, Text text, Context context) throws IOException, InterruptedException {

			String line = text.toString();
			
			if (line.contains(">>")) {
				String split[] = line.split(">>");
				context.write(new Text(split[0].trim()), new Text("Model:" + split[1].trim()));
			} else {
				sendTestData(line, context);
			}
		}

		public void sendTestData(String line, Context context) throws IOException, InterruptedException {
			line = line.replaceAll("\"", "");
			line = line.replaceAll(", ", "#");
			String[] airlinedata = line.split(",");
			Integer month = null;
			Integer day_of_month = null;
			Integer day_of_week = null;
			String carrierID = "";
			Integer originAirportID = null;
			Integer year = null;
			String flightDate = "";
			Integer destAirportID = null;
			Integer CRSDeptTime = null;
			Integer CRSArrTime = null;
			String flightNumber = "";
			Boolean emptyRow = true;
			Boolean holiday = true;
			Boolean flag = true;

			try {
				month = Integer.valueOf(airlinedata[3]);
				day_of_month = Integer.valueOf(airlinedata[4]);
				day_of_week = Integer.valueOf(airlinedata[5]);
				carrierID = airlinedata[7];
				year = Integer.valueOf(airlinedata[1]);
				flightDate = airlinedata[6];
				CRSDeptTime = Integer.valueOf(airlinedata[30]);
				CRSArrTime = Integer.valueOf(airlinedata[41]);
				originAirportID = Integer.valueOf(airlinedata[12]);
				destAirportID = Integer.valueOf(airlinedata[21]);
				// year = Integer.valueOf(airlinedata[0]);
				flightNumber = airlinedata[11];
				//flightNumber = airlinedata[10];
				
				String[] checkDate ;
				String checkMonth; 
				String checkDay;
				checkDate = flightDate.split("-");
				checkMonth = checkDate[1];
				checkDay = checkDate[2];
				if(holidays(checkDay,checkMonth)){
					holiday = true;
				}
				else{
					holiday = false;
				}

			} catch (Exception e) {
				// e.printStackTrace();
				emptyRow = false;

			}
			if (airlinedata.length == 112 && emptyRow) {
				String outValue = day_of_month.toString() + " " +year+" "+ day_of_week.toString() + " "
						+ carrierID + " " + originAirportID.toString() + " " + destAirportID.toString() + " "
						+ CRSDeptTime.toString() + " " + CRSArrTime.toString() + " " + flightNumber + " " +flightDate +" "
						+ flag.toString() + " " + holiday.toString();
				Text outvalue = new Text(outValue);
				Text outkey = new Text(month+"");
				

				context.write(outkey, outvalue);

			}

		}
		private boolean holidays(String checkDay, String checkMonth) {
			int day = Integer.valueOf(checkDay);
			int month = Integer.valueOf(checkMonth);
			if(month == 12 && (day == 31 | day ==25)){
				return true;
			}
			else if((month == 7 && day ==4) | (month ==1 && day == 1) | (month == 11 && day == 24)){
				return true;
			}
			
			
			
			return false;
		}

	}
	
	public static class OriginReducer extends Reducer<Text, Text, Text, Text> {

		

		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

			ArrayList<Attribute> attribute = new ArrayList<Attribute>();
			String modelData = "";
			attribute.add(new Attribute("day_of_month"));
			attribute.add(new Attribute("day_of_week"));
			attribute.add(new Attribute("originAirportID"));
			attribute.add(new Attribute("destAirportID"));
			attribute.add(new Attribute("CRSDeptTime"));
			attribute.add(new Attribute("CRSArrTime"));
			//attribute.add(new Attribute("Flight_No"));
			Attribute carrierCode = new Attribute("carrierID");

			List<String> flagList = new ArrayList<String>(2);
			flagList.add("true");
			flagList.add("false");
			Attribute delay = new Attribute("delay", flagList);

			List<String> holidayList = new ArrayList<String>(2);
			holidayList.add("true");
			holidayList.add("false");
			Attribute holiday = new Attribute("holiday", holidayList);

			attribute.add(carrierCode);
			attribute.add(delay);
			attribute.add(holiday);

			Instances TestSet = new Instances("TestModel", attribute, 0);
			TestSet.setClassIndex(7);
			Classifier classifier1 = null;
			ObjectInputStream ois = null;
			String flightnumber = "";
			String flightDate = "" ;
			String departureTime = "";
			for (Text value : values) {
				String line = value.toString();
				// Model Data
				if (line.contains("Model")) {
					try {
						modelData = (line.split(":")[1].trim());
						byte[] barr = Base64.getDecoder().decode(modelData);
						InputStream stream = new ByteArrayInputStream(barr);

						//
						classifier1 = (Classifier) (SerializationHelper.read(stream));

					} catch (Exception e) {
						// TODO Auto-generated catch block
						// System.out.println(modelData);;
						e.printStackTrace();
					}

				}
				// Test Data
				else {
					String[] valueSplitter = line.split(" ");
					//System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + line);
					//System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + valueSplitter.toString());
					flightnumber = valueSplitter[8];
					flightDate = valueSplitter[9];
					departureTime = valueSplitter[6];
					Instance iExample = new DenseInstance(10);
					iExample.setValue(attribute.get(0), Integer.parseInt(valueSplitter[0])); // day_of_month
					iExample.setValue(attribute.get(1), Integer.parseInt(valueSplitter[2])); // day_of_week
					iExample.setValue(attribute.get(2), Integer.parseInt(valueSplitter[4])); // originAirportID
					iExample.setValue(attribute.get(3), Integer.parseInt(valueSplitter[5])); // destAirportID
					iExample.setValue(attribute.get(4), Integer.parseInt(valueSplitter[6])); // CRSDeptTime
					iExample.setValue(attribute.get(5), Integer.parseInt(valueSplitter[7])); // CRSArrTime
					//iExample.setValue(attribute.get(6), Integer.parseInt(valueSplitter[8])); // Flight_no
					iExample.setValue(attribute.get(6), valueSplitter[3].hashCode()); // CarrierID
					iExample.setValue(attribute.get(7), Boolean.parseBoolean(valueSplitter[10]) ? 1 : 0); // flag
					iExample.setValue(attribute.get(8), Boolean.parseBoolean(valueSplitter[11]) ? 1 : 0); // holiday

					TestSet.add(iExample);
				}
			}
			//System.out.println("\n\n\ " + key.toString() + "     "+TestSet.size()+"   " + classifier1 + "\n\n\n\n");
			try { if(classifier1 != null)
				{
				Boolean result;
					for (int i = 1; i < TestSet.size(); i++) {
						Instance ins = TestSet.get(i);
						double [] predicted = classifier1.distributionForInstance(ins);
						if(predicted[0] > predicted[1]) { 
							result = true;
						}else
						{
							result =false;
						}
						String Outkey = flightnumber + "_" + flightDate + "_" + departureTime ;
						context.write(new Text(Outkey), new Text(result + ""));
					}

				}
			} catch (Exception e) {
				System.out.println(TestSet.size());
				e.printStackTrace();
			}

		}

	} // end of originReducer

	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		if (otherArgs.length != 3) {
			System.err.println("Usage: Prediction <input-path> <output-path>");
			System.exit(1);
			return;
		}

		@SuppressWarnings("deprecation")
		Job job = new Job(conf, "Average Flight Price");
		job.setJarByClass(Test.class);
		job.setMapperClass(OriginMapper.class);
		job.setReducerClass(OriginReducer.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		Path[] array = new Path[2];
		array[0] = new Path(otherArgs[0]);
		array[1] = new Path(otherArgs[1]);
		System.out.println(array.toString());
		FileInputFormat.setInputPaths(job, array);
		FileOutputFormat.setOutputPath(job, new Path(otherArgs[2]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}