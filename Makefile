#Author: Akanksha and Saahil
hstart:
	start-dfs.sh
	start-yarn.sh
	mr-jobhistory-daemon.sh start historyserver

copy:
	aws s3 cp s3://mrclassvitek/a6history Mishra_Singla_A7/Prediction/history --recursive
	aws s3 cp s3://mrclassvitek/a6test Mishra_Singla_A7/Prediction/ --recursive
	aws s3 cp s3://mrclassvitek/a6validate Mishra_Singla_A7/Prediction/ --recursive
	gzip -d Mishra_Singla_A7/Prediction/98redacted.csv.gz
	gzip -d Mishra_Singla_A7/Prediction/98validate.csv.gz

akanksha:
	bin/hadoop fs -mkdir -p /user/akanksha
	bin/hadoop fs -mkdir -p /user/akanksha/input
	bin/hadoop dfs -copyFromLocal Mishra_Singla_A7/Prediction/history/ /user/akanksha/input/

clean:
	rm -f *.class
	rm -f *.jar
	rm -rf output
	bin/hadoop fs -rm -r -f output

jar:
	javac -cp commons-cli-1.3.1.jar:hadoop-common-2.3.0.jar:hadoop-mapreduce-client-core-2.0.2-alpha.jar:hadoop-test-1.2.1.jar:weka-3.7.3.jar  Model.java
	jar cvf Prediction1.jar *.class
	rm -f *.class
	javac -cp commons-cli-1.3.1.jar:hadoop-common-2.3.0.jar:hadoop-mapreduce-client-core-2.0.2-alpha.jar:hadoop-test-1.2.1.jar:weka-3.7.3.jar  Test.java
	jar cvf Prediction2.jar *.class

pseudo:
	bin/hadoop jar /Mishra_Singla_A7/Prediction/Prediction1.jar /user/akanksha/input /user/akanksha/output1
	bin/hadoop jar Mishra_Singla_A7/Prediction/Prediction2.jar /user/akanksha/output1/part-r-* /Mishra_Singla_A7/98redacted.csv /user/akanksha/output2
	bin/hadoop dfs -copyToLocal /user/akanksha/output2 Mishra_Singla_A7

merge: 
	# Merge two files, validated file as well as output predicted file.
	awk -F, 'FNR==NR{a[$$1]=$$2;next}{ print $$0, a[$$1]}' Mishra_Singla_A7/Prediction/output2 Mishra_Singla_A7/Prediction/98validate.csv > output1.txt
	# Add comma between two columns
	awk '{ $$1=$$1","; print }' output1.txt > output2.txt
	# Remove spaces between the columns
	awk '{$$1=$$1}1' OFS= output2.txt > final_output.txt
	rm output*

confusion:
	 python matrix.py

emr:
	aws s3 rm s3://mishrasingla02/Prediction1 --recursive
	aws emr create-cluster --release-label emr-4.3.0 --name "CLI Test Cluster" --instance-groups InstanceGroupType=MASTER,InstanceCount=1,InstanceType=m3.xlarge                   InstanceGroupType=CORE,InstanceCount=2,InstanceType=m3.xlarge --steps Type=CUSTOM_JAR,Name="CLI Test JAR Step",ActionOnFailure=CONTINUE,Jar=s3://mishrasingla02/Prediction1.jar,Args=[s3n://mishrasingla02/input,s3n://mishrasingla02/Prediction1] --log-uri s3://mishrasingla02/logs --service-role EMR_DefaultRole --ec2-attributes InstanceProfile=EMR_EC2_DefaultRole,AvailabilityZone=us-west-2a --enable-debugging
	echo "Sleeping for about 10 mins"
	sleep 900
	aws s3 rm s3://mishrasingla02/Prediction2 --recursive
	aws emr create-cluster --release-label emr-4.3.0 --name "CLI Test Cluster" --instance-groups InstanceGroupType=MASTER,InstanceCount=1,InstanceType=m3.xlarge                   InstanceGroupType=CORE,InstanceCount=2,InstanceType=m3.xlarge --steps Type=CUSTOM_JAR,Name="CLI Test JAR Step",ActionOnFailure=CONTINUE,Jar=s3://mishrasingla02/Prediction2.jar,Args=[s3n://mishrasingla02/input,s3://mrclassvitek/a6validate,s3n://mishrasingla02/Prediction2] --log-uri s3://mishrasingla02/logs --service-role EMR_DefaultRole --ec2-attributes InstanceProfile=EMR_EC2_DefaultRole,AvailabilityZone=us-west-2a --enable-debugging

outputaws:
	# copying file from s3 to local
	rm -rf Mishra_Singla_A7/Prediction2
	rm -rf *.txt
	aws s3 cp s3://mishrasingla02/Prediction2 Mishra_Singla_A7/S3Prediction  --recursive

aws-merge:
	# Merge two files, validated file as well as output predicted file.
	awk -F, 'FNR==NR{a[$$1]=$$2;next}{ print $$0, a[$$1]}' Mishra_Singla_A7/S3Prediction Mishra_Singla_A7/Prediction/98validate.csv > output1.txt
	# Add comma between two columns
	awk '{ $$1=$$1","; print }' output1.txt > output2.txt
	# Remove spaces between the columns
	awk '{$$1=$$1}1' OFS= output2.txt > final_output.txt
	rm output*
	python matrix.py	






