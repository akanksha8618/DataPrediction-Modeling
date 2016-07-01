FF = 0
TT = 0
FT = 0
TF = 0
with open("final_output.txt") as file:

	for line in file:
		splitter=list()
		splitter=line.split(",");
		
		if splitter[1]=="TRUE" and splitter[2]=="TRUE\n":
			TT=TT+1
		elif splitter[1]=="TRUE" and splitter[2]=="FALSE\n":
			TF=TF+1
		elif splitter[1]=="FALSE" and splitter[2]=="TRUE\n":
			FT=FT+1
		elif splitter[1]=="FALSE" and splitter[2]=="FALSE\n":
			FF=FF+1
	percentage=(TT+FF)/(float)(TT+TF+FT+FF)*100
	print("Percentage Accurate: " , percentage) 
	print("True True : " ,TT)
	print("True False: " , TF)
	print("False True: " , FT)
	print('False False: ' , FF)
