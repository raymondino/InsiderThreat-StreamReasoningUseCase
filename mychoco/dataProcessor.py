# dataProcessor.py
# Extracts, combines, and annotates data in one step
# Run this script with command line input UserID
# Generates results.nt

import sys
import data_combinator_new as combinator
import annotation as annotator
from globals import *
import datetime
path = '../data-r6.2/'

def extract(user):
	usr_device = open('intermediate/'+user+'_device.csv', 'w+');
	usr_logon = open('intermediate/'+user+'_logon.csv', 'w+');
	usr_file = open('intermediate/'+user+'_file.csv', 'w+');
	usr_email = open('intermediate/'+user+'_email.csv', 'w+');
	usr_http = open('intermediate/'+user+'_http.csv', 'w+');

	device = open(path+'device.csv', 'r')
	for d in device:
		if d.split(',')[2] == user:
			usr_device.write('device,' + d);
	device.close();
	usr_device.close();
	print 'Device extract done.'

	logon = open(path+'logon.csv', 'r')
	for l in logon:
		if l.split(',')[2] == user :
			usr_logon.write('logon,' + l);
	logon.close();
	usr_logon.close();
	print 'Logon extract done.'

	file = open(path+'file.csv', 'r')
	for f in file:
		if f.split(',')[2] == user :
			usr_file.write('file,' + f);
	file.close();
	usr_file.close();
	print 'File extract done.'

	email = open(path+'email.csv', 'r')
	for e in email:
		if e.split(',')[2] == user :
			usr_email.write('email,' + e);
	email.close();
	usr_email.close();
	print 'Email extract done.'

	http = open(path+'http.csv', 'r')
	for h in http:
		if h.split(',')[2] == user :
			usr_http.write('http,' + h);
	http.close();
	usr_http.close();
	print 'HTTP extract done.'

def combine(user):
	fileList = [open('intermediate/'+user+'_device.csv'),open('intermediate/'+user+'_email.csv'),\
				open('intermediate/'+user+'_file.csv'),open('intermediate/'+user+'_http.csv'),open('intermediate/'+user+'_logon.csv')]
	outfile = open('intermediate/'+user+'_aggregated.csv','w')
	combinator.combine(fileList,outfile)
	for f in fileList:
		f.close()
	outfile.close()

def annotate(user):
	f = open('intermediate/'+user+'_aggregated.csv')
	outfile = open('results.nt','w')
	userID = f.readline().split(',')[3]
	print >>outfile, '<%s%s> <%sisInvolvedIn> <%sevent%s>.' %(ex,userID,ex,ex,userID)
	f.seek(0,0)
	deviceUsageCounter = 0
	isDeviceConnected = False
	connectedDevice = ''
	for record in f:
	    type = record[:record.find(',')]
	    if type == 'logon':
	        record = record.strip().split(',')
	        if record[5] == 'Logoff':
	            connectedDevice = ''
	        annotator.logon(record,outfile)
	    elif type == 'device':
	        record = record.strip().split(',')
	        if record[6] == 'Connect':
	            deviceUsageCounter += 1
	            connectedDevice = record[1][1:len(record[1])-1]  # id of this record
	        elif record[6] == 'Disconnect':
	            connectedDevice = ''
	        annotator.device(record,outfile)
	    elif type == 'email':
	        content = record[record.find('"'):len(record)-1].replace('"','')
	        record = record[:record.find('"')].split(',')
	        record.append(content)
	        annotator.email(record,outfile)
	    elif type == 'file':
	        content = record[record.find('"'):len(record)-1].replace('"','')
	        record = record[:record.find('"')].split(',')
	        record.append(content)
	        annotator.file(record,outfile)
	        if connectedDevice:
	            id = record[1][1:len(record[1])-1]
	            timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
	            print >>outfile, '<%s%s> <%sstartsNoEarlierThanEndingOf> <%s%s>.|%s' %(ex,id,ex,ex,connectedDevice,annotator.tsToStr(timestamp))
	    elif type == 'http':
	        content = record[record.find('"'):len(record)-1].replace('"','')
	        record = record[:record.find('"')].split(',')
	        record.append(content)
	        annotator.http(record,outfile)
	    else:
	        print >>outfile, 'unknown type:', type
	        raise

	if deviceUsageCounter > usbDriveUsageFrequency:
	    timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
	    print >>outfile, '<%s%s> <%s> <%sExcessiveRemovableDriveUser>.|%s' %(ex,userID,a,ex,annotator.tsToStr(timestamp))
	f.close()
	outfile.close()

if __name__ == '__main__':
	if len(sys.argv)<2:
		print 'USAGE: [filename] [userid]'
		sys.exit(0)

	userid = sys.argv[1]
	extract(userid)
	print 'Extract done.'
	combine(userid)
	print 'Combine done.'
	annotate(userid)
	print 'Annotate done'
