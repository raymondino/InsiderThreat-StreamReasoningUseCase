from datetime import datetime

# merge read_file1 & read_file2 into write_file according to data's datetime in each file 
def combine(f1, f2, read_file1, read_file2, write_file):
	a = read_file1.readline();
	b = read_file2.readline();

	flag1 = f1;
	flag2 = f2;
	while((not flag1) and (not flag2)) : # don't read the bottom of both files
		
		if(not a): # if reaches read_file1 bottom
			flag1 = True; 
			break;
		if(not b): # if reaches read_file2 bottom
			flag2 = True; 
			break;
		try:

			if datetime.strptime(a.split(',')[2], "%m/%d/%Y %H:%M:%S") < datetime.strptime(b.split(',')[2], "%m/%d/%Y %H:%M:%S") :
				write_file.write(a);
				a = read_file1.readline();
				continue;
		
			elif datetime.strptime(a.split(',')[2], "%m/%d/%Y %H:%M:%S") > datetime.strptime(b.split(',')[2], "%m/%d/%Y %H:%M:%S") :
				write_file.write(b);
				b = read_file2.readline();
				continue;
		
			else :
				write_file.write(a);
				a = read_file1.readline();
				write_file.write(b);
				b = read_file2.readline();
				continue;

		except Exception:
			print a;
			print b;
	
	if not flag1: # if read_file1 is unfinished
		while(not not a): # if a is "", "not a" gives true. so if a is not empty, "not not a" gives true.
			write_file.write(a);
			a = read_file1.readline();
	
	if not flag2: # if read_file2 is unfinished
		while(not not b):
			write_file.write(b);
			b = read_file2.readline();

	return write_file; # return merged file

user = "ACM2278";

# temp1 = combine(False, False, open(user + "_device.csv", 'r'), open(user + "_logon.csv", 'r'), open(user + "_temp1.csv", 'w'))
# print "temp1 done"
# temp1.close();
# temp2 = combine(False, False, open(user + "_file.csv", 'r'), open(user + "_temp1.csv", 'r'), open(user + "_temp2.csv", 'w'))
# print "temp2 done"
# temp2.close();
# temp3 = combine(False, False, open(user + "_email.csv", 'r'), open(user + "_temp2.csv", 'r'), open(user + "_temp3.csv", 'w'))
# print "temp3 done"
# temp3.close();
temp4 = combine(False, False, open(user + "_http.csv", 'r'), open(user + "_temp3.csv", 'r'), open(user + "-aggregated.csv", 'w'))
temp4.close();
print "temp4 done"