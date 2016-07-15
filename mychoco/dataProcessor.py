# dataProcessor.py
import sys, datetime
from globals import *
MAXTIME = datetime.datetime(9999, 12, 31, 23, 59, 59)
path = '../data-r6.2/'

# Need to change this for each user
dailyStart = datetime.time(7,27-15,12)
dailyEnd = datetime.time(17,34+15,48)

#
# returns the index of the line having the earliest timestamp
def findEarliest(firstLines):
    minIndex = -1
    minTime = MAXTIME
    for i in range(len(firstLines)):
        if (firstLines[i]):
            line = firstLines[i].strip().split(',')
            timestamp = datetime.datetime.strptime(line[2],'%m/%d/%Y %H:%M:%S')
            if timestamp < minTime:
                minTime = timestamp
                minIndex = i
    return minIndex

def allEmpty(firstLines):
    for line in firstLines:
        if line:
            return False
    return True

# combines the files in fileList ordered by timestamp of each line and saves the combined file in outFile
def combineFiles(fileList,outFile):
    firstLines = []
    for eachFile in fileList:
        firstLines.append(eachFile.readline())
    while not allEmpty(firstLines):
        i= findEarliest(firstLines)
        outFile.write(firstLines[i])
        firstLines[i] = fileList[i].readline()

#
# converts a timestamp to a string
def tsToStr(timestamp):
    return str(timestamp.date())+'T'+str(timestamp.time())

def logon(record,outfile):
    id = record[1][1:len(record[1])-1]
    timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
    dailyStart = dailyStartDic[record[3]]
    dailyEnd = dailyEndDic[record[3]]
    print >>outfile, '<%sevent%s> <%shasAction> <%slogon_%s>.' %(ex,record[3],ex,ex,id)
    print >>outfile, '<%slogon_%s> <%s> <%s%sAction>.|%s' %(ex,id,a,ex,record[5],tsToStr(timestamp))
    print >>outfile, '<%slogon_%s> <%shasTimestamp> "%s-05:00"^^<%sdateTime>.|%s' %(ex,id,ex,tsToStr(timestamp),xsd,tsToStr(timestamp))
    if timestamp.time() < dailyStart or timestamp.time() > dailyEnd:
        print >>outfile, '<%slogon_%s> <%s> <%sAfterHourAction>.|%s' %(ex,id,a,ex,tsToStr(timestamp))
    else:
        print >>outfile, '<%slogon_%s> <%s> <%sInHourAction>.|%s' %(ex,id,a,ex,tsToStr(timestamp))
    print >>outfile, '<%slogon_%s> <%shasActor> <%s%s>.|%s' %(ex,id,ex,ex,record[3],tsToStr(timestamp))
    print >>outfile, '<%slogon_%s> <%sisPerformedOnPC> <%s%s>.|%s' %(ex,id,ex,ex,record[4],tsToStr(timestamp))

def device(record,outfile):
    id = record[1][1:len(record[1])-1]
    timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
    dailyStart = dailyStartDic[record[3]]
    dailyEnd = dailyEndDic[record[3]]
    print >>outfile, '<%sevent%s> <%shasAction> <%sdevice_%s>.' %(ex,record[3],ex,ex,id)
    print >>outfile, '<%sdevice_%s> <%s> <%s%sAction>.|%s' %(ex,id,a,ex,'Disk'+record[6]+'ion',tsToStr(timestamp))
    print >>outfile, '<%sdevice_%s> <%shasTimestamp> "%s-05:00"^^<%sdateTime>.|%s' %(ex,id,ex,tsToStr(timestamp),xsd,tsToStr(timestamp))
    if timestamp.time() < dailyStart or timestamp.time() > dailyEnd:
        print >>outfile, '<%sdevice_%s> <%s> <%sAfterHourAction>.|%s' %(ex,id,a,ex,tsToStr(timestamp))
    else:
        print >>outfile, '<%sdevice_%s> <%s> <%sInHourAction>.|%s' %(ex,id,a,ex,tsToStr(timestamp))
    print >>outfile, '<%sdevice_%s> <%shasActor> <%s%s>.|%s' %(ex,id,ex,ex,record[3],tsToStr(timestamp))
    print >>outfile, '<%sdevice_%s> <%sisPerformedOnPC> <%s%s>.|%s' %(ex,id,ex,ex,record[4],tsToStr(timestamp))
    if (record[6]=='Connect'):
        print >>outfile, '<%sdevice_%s> <%sisPerformedWithRemovableDisk> <%sdevice_%s_disk>.|%s' %(ex,id,ex,ex,id,tsToStr(timestamp))
        print >>outfile, '<%sdevice_%s_disk> <%shasFileTree> "%s"^^<%sstring>.|%s' %(ex,id,ex,record[5].replace('\\','_'),xsd,tsToStr(timestamp))

# email, id, date, user, pc, to, cc, bcc, from, activity, size, attachment, content
#   0,    1,   2,    3,   4,  5,  6,  7,    8,       9,    10,      11
def email(record,outfile):
    content = record[-1]
    id = record[1][1:len(record[1])-1]
    timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
    dailyStart = dailyStartDic[record[3]]
    dailyEnd = dailyEndDic[record[3]]
    print >>outfile, '<%sevent%s> <%shasAction> <%semail_%s>.' %(ex,record[3],ex,ex,id)
    print >>outfile, '<%semail_%s> <%s> <%sEmail%sAction>.|%s' %(ex,id,a,ex,record[9],tsToStr(timestamp))
    print >>outfile, '<%semail_%s> <%shasTimestamp> "%s-05:00"^^<%sdateTime>.|%s' %(ex,id,ex,tsToStr(timestamp),xsd,tsToStr(timestamp))
    if timestamp.time() < dailyStart or timestamp.time() > dailyEnd:
        print >>outfile, '<%semail_%s> <%s> <%sAfterHourAction>.|%s' %(ex,id,a,ex,tsToStr(timestamp))
    else:
        print >>outfile, '<%semail_%s> <%s> <%sInHourAction>.|%s' %(ex,id,a,ex,tsToStr(timestamp))
    print >>outfile, '<%semail_%s> <%shasActor> <%s%s>.|%s' %(ex,id,ex,ex,record[3],tsToStr(timestamp))
    print >>outfile, '<%semail_%s> <%sisPerformedOnPC> <%s%s>.|%s' %(ex,id,ex,ex,record[4],tsToStr(timestamp))
    if record[5]:
        toList = record[5].split(';')
        for item in toList:
            print >>outfile, '<%semail_%s> <%sto> <%s%s>.|%s'%(ex,id,ex,ex,item,tsToStr(timestamp))
            if item[item.find('@'):] == '@dtaa.com':
                print >>outfile, '<%semail_%s> <%s> <%sInternalEmailAddress>.|%s' %(ex,item,a,ex,tsToStr(timestamp))
            else:
                print >>outfile, '<%semail_%s> <%s> <%sNotInternalEmailAddress>.|%s' %(ex,item,a,ex,tsToStr(timestamp))
    if record[6]:
        ccList = record[6].split(';')
        for item in ccList:
            print >>outfile, '<%semail_%s> <%scc> <%s%s>.|%s'%(ex,id,ex,ex,item,tsToStr(timestamp))
            if item[item.find('@'):] == '@dtaa.com':
                print >>outfile, '<%semail_%s> <%s> <%sInternalEmailAddress>.|%s' %(ex,item,a,ex,tsToStr(timestamp))
            else:
                print >>outfile, '<%semail_%s> <%s> <%sNotInternalEmailAddress>.|%s' %(ex,item,a,ex,tsToStr(timestamp))
    if record[7]:
        bccList = record[7].split(';')
        for item in bccList:
            print >>outfile, '<%semail_%s> <%sbcc> <%s%s>.|%s'%(ex,id,ex,ex,item,tsToStr(timestamp))
            if item[item.find('@'):] == '@dtaa.com':
                print >>outfile, '<%semail_%s> <%s> <%sInternalEmailAddress>.|%s' %(ex,item,a,ex,tsToStr(timestamp))
            else:
                print >>outfile, '<%semail_%s> <%s> <%sNotInternalEmailAddress>.|%s' %(ex,item,a,ex,tsToStr(timestamp))

    print >>outfile, '<%semail_%s> <%sfrom> <%s%s>.|%s'%(ex,id,ex,ex,record[8],tsToStr(timestamp))
    if record[8][record[8].find('@'):] == '@dtaa.com':
        print >>outfile, '<%semail_%s> <%s> <%sInternalEmailAddress>.|%s' %(ex,record[8],a,ex,tsToStr(timestamp))
    else:
        print >>outfile, '<%semail_%s> <%s> <%sNotInternalEmailAddress>.|%s' %(ex,record[8],a,ex,tsToStr(timestamp))

    print >>outfile, '<%semail_%s> <%shasEmailSize> "%s bytes"^^<%sstring>.|%s'%(ex,id,ex,record[10],xsd,tsToStr(timestamp))
    if record[11]:
        attachmentList = record[11].split(';')
        for i in range(len(attachmentList)):
            filename = attachmentList[i][:attachmentList[i].find('(')].replace('\\','_')
            attachmentSize = attachmentList[i][attachmentList[i].find('(')+1:item.find(')')]
            print >>outfile, '<%semail_%s> <%shasEmailAttachment> <%s%s>.|%s'%(ex,id,ex,ex,filename,tsToStr(timestamp))
            print >>outfile, '<%sattachment%s> <%shasSize> "%s bytes"^^<%sstring>.|%s'%(ex,str(i+1),ex,attachmentSize,xsd,tsToStr(timestamp))
    print >>outfile, '<%semail_%s> <%shasContent> "%s"^^<%sstring>.|%s'%(ex,id,ex,content,xsd,tsToStr(timestamp))

# file, id, date, user, pc, filename, activity, to_removable_media, from_removable_media, content
#    0,  1,   2,   3,   4,    5,         6,               7,                8,               9
def file(record,outfile):
    content = record[-1]
    id = record[1][1:len(record[1])-1]
    timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
    dailyStart = dailyStartDic[record[3]]
    dailyEnd = dailyEndDic[record[3]]
    print >>outfile, '<%sevent%s> <%shasAction> <%sfile_%s>.' %(ex,record[3],ex,ex,id)
    print >>outfile, '<%sfile_%s> <%s> <%sFile%sAction>.|%s' %(ex,id,a,ex,record[6][5:],tsToStr(timestamp))
    print >>outfile, '<%sfile_%s> <%shasTimestamp> "%s-05:00"^^<%sdateTime>.|%s' %(ex,id,ex,tsToStr(timestamp),xsd,tsToStr(timestamp))
    if timestamp.time() < dailyStart or timestamp.time() > dailyEnd:
        print >>outfile, '<%sfile_%s> <%s> <%sAfterHourAction>.|%s' %(ex,id,a,ex,tsToStr(timestamp))
    else:
        print >>outfile, '<%sfile_%s> <%s> <%sInHourAction>.|%s' %(ex,id,a,ex,tsToStr(timestamp))
    print >>outfile, '<%sfile_%s> <%shasActor> <%s%s>.|%s' %(ex,id,ex,ex,record[3],tsToStr(timestamp))
    print >>outfile, '<%sfile_%s> <%sisPerformedOnPC> <%s%s>.|%s' %(ex,id,ex,ex,record[4],tsToStr(timestamp))
    filename = record[5].replace('\\','_')
    print >>outfile, '<%sfile_%s> <%shasFile> <%s%s>.|%s'%(ex,id,ex,ex,filename,tsToStr(timestamp))
    if record[7]=='True':
        print >>outfile, '<%sfile_%s_file> <%s> <%sFileToRemovableMedia>.|%s' %(ex,id,a,ex,tsToStr(timestamp))
    else:
        print >>outfile, '<%sfile_%s_file> <%s> <%sNotFileToRemovableMedia>.|%s' %(ex,id,a,ex,tsToStr(timestamp))
    if record[8]=='True':
        print >>outfile, '<%sfile_%s_file> <%s> <%sFileFromRemovableMedia>.|%s' %(ex,id,a,ex,tsToStr(timestamp))
    else:
        print >>outfile, '<%sfile_%s_file> <%s> <%sNotFileFromRemovableMedia>.|%s' %(ex,id,a,ex,tsToStr(timestamp))
    print >>outfile, '<%sfile_%s_file> <%shasContent> "%s"^^<%sstring>.|%s' %(ex,id,ex,content,xsd,tsToStr(timestamp))

# http, id, date, user, pc, url, activity, content
#  0,    1,   2,   3,   4,   5,     6,        7
def http(record,outfile):
    content = record[-1]
    id = record[1][1:len(record[1])-1]
    timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
    dailyStart = dailyStartDic[record[3]]
    dailyEnd = dailyEndDic[record[3]]
    print >>outfile, '<%sevent%s> <%shasAction> <%shttp_%s>.' %(ex,record[3],ex,ex,id)
    print >>outfile, '<%shttp_%s> <%s> <%s%sAction>.|%s' %(ex,id,a,ex,record[6].replace(' ',''),tsToStr(timestamp))
    print >>outfile, '<%shttp_%s> <%shasTimestamp> "%s-05:00"^^<%sdateTime>.|%s' %(ex,id,ex,tsToStr(timestamp),xsd,tsToStr(timestamp))
    if timestamp.time() < dailyStart or timestamp.time() > dailyEnd:
        print >>outfile, '<%shttp_%s> <%s> <%sAfterHourAction>.|%s' %(ex,id,a,ex,tsToStr(timestamp))
    else:
        print >>outfile, '<%shttp_%s> <%s> <%sInHourAction>.|%s' %(ex,id,a,ex,tsToStr(timestamp))
    print >>outfile, '<%shttp_%s> <%shasActor> <%s%s>.|%s' %(ex,id,ex,ex,record[3],tsToStr(timestamp))
    print >>outfile, '<%shttp_%s> <%sisPerformedOnPC> <%s%s>.|%s' %(ex,id,ex,ex,record[4],tsToStr(timestamp))
    print >>outfile, '<%shttp_%s> <%shasURL> <%s>.|%s' %(ex,id,ex,record[5],tsToStr(timestamp))
    domainName = record[5][:record[5].find('/',7)+1]
    if domainName in cloudStorageWebsites:
        print >>outfile, '<%s> <%swhoseDomainNameIsA> <%scloudstoragewebsite>.|%s' %(record[5],ex,ex,tsToStr(timestamp))
    elif domainName in hacktivistWebsites:
        print >>outfile, '<%s> <%swhoseDomainNameIsA> <%shacktivistwebsite>.|%s' %(record[5],ex,ex,tsToStr(timestamp))
    elif domainName in jobHuntingWebsites:
        print >>outfile, '<%s> <%swhoseDomainNameIsA> <%sjobhuntingwebsite>.|%s' %(record[5],ex,ex,tsToStr(timestamp))
    else:
        print >>outfile, '<%s> <%s> <%sneuturalwebsite>.|%s' %(record[5],a,ex,tsToStr(timestamp))
    print >>outfile, '<%shttp_%s> <%shasContent> "%s"^^<%sstring>.|%s' %(ex,id,ex,content,xsd,tsToStr(timestamp))


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
	combineFiles(fileList,outfile)
	for f in fileList:
		f.close()
	outfile.close()

def annotate(user):
	f = open('intermediate/'+user+'_aggregated.csv')
	outfile = open(user+'-annotation.txt','w')
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
	        logon(record,outfile)
	    elif type == 'device':
	        record = record.strip().split(',')
	        if record[6] == 'Connect':
	            deviceUsageCounter += 1
	            connectedDevice = record[1][1:len(record[1])-1]  # id of this record
	        elif record[6] == 'Disconnect':
	            connectedDevice = ''
	        device(record,outfile)
	    elif type == 'email':
	        content = record[record.find('"'):len(record)-1].replace('"','')
	        record = record[:record.find('"')].split(',')
	        record.append(content)
	        email(record,outfile)
	    elif type == 'file':
	        content = record[record.find('"'):len(record)-1].replace('"','')
	        record = record[:record.find('"')].split(',')
	        record.append(content)
	        file(record,outfile)
	        if connectedDevice:
	            id = record[1][1:len(record[1])-1]
	            timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
	            print >>outfile, '<%s%s> <%sstartsNoEarlierThanEndingOf> <%s%s>.|%s' %(ex,id,ex,ex,connectedDevice,tsToStr(timestamp))
	    elif type == 'http':
	        content = record[record.find('"'):len(record)-1].replace('"','')
	        record = record[:record.find('"')].split(',')
	        record.append(content)
	        http(record,outfile)
	    else:
	        print >>outfile, 'unknown type:', type
	        raise

	if deviceUsageCounter > usbDriveUsageFrequency[user]:
	    timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
	    print >>outfile, '<%s%s> <%s> <%sExcessiveRemovableDriveUser>.|%s' %(ex,userID,a,ex,tsToStr(timestamp))
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
	print 'Annotate done.'
