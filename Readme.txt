Author: Akanksha Mishra, Saahil Singla

Instructions for building and executing the code:


1. Program Dependencies  
		   - Java 1.8
           - We have used latex to generate our report.
           - You should have following jars for successful compilation of the java files
           		* commons-cli-1.3.1.jar
           		* hadoop-common-2.3.0.jar
           		* hadoop-mapreduce-client-core-2.0.2-alpha.jar
           		* hadoop-test-1.2.1.jar:weka-3.7.3.jar
		  
2. Commands to run:
	For local : make clean
		    	make copy
	            make akanksha
	            make jar
	            make pseudo
	            make merge
	            make confusion

	For aws :   make emr
			    make outputaws
			    make aws-merge


****** To run the program locally ******* 
The first file Model.java is converted into jar and run on hadoop by giving all 36 history files as input.
The second file Test.java is run on hadoop by giving output of Model.java and 98redacted.csv as input
The final output is saved in folder Mishra_Singla_A7/Predictions and further commands (make merge and make confusion) are run to get the final output.
			  
****** To run the program on EMR ******* 
	make changes in the makefile and replace mishrasingla02 bucket with your bucket.

	** Type aws configure in command prompt.

	** Set your Amazon credentials and default output format as json in prompted fields in command prompt as shown below:
		AWS Access Key ID : 
		AWS Secret Access Key : 
		Default region name : us-west-2
		Default output format : json

	** Upload the required Jar files of Prediction in the input folder of your S3 bucket.
	** Make the jar for scala file by running command “sbt package”
	** Replace '{bucket_name} with your bucketname
	** Make sure HADOOP_CLASSPATH is set correctly before running any command.
		Command: export HADOOP_CLASSPATH=.:`hadoop classpath`

NOTE: If EMR is not working for you, try to check the parameters in Makefile.