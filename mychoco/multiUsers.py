# multiUsers.py
import globals
path = '../data-r6.2/'

#######################
# EXTRACT
def multiUserExtractHelper(actionType, userList):
	inFile = open(path+actionType+'.csv')
	outFile = open('intermediate/multi_users_'+actionType+'.csv', 'w')
	for line in inFile:
		if line.split(',')[2] in userList:
			outFile.write(actionType+', '+ line)
	outFile.close()
	inFile.close()
	print actionType, 'extract done.'

def multiUserExtract(userList):
	usr_device = open('intermediate/multi_users_device.csv', 'w+');
	usr_logon = open('intermediate/multi_users_logon.csv', 'w+');
	usr_file = open('intermediate/multi_users_file.csv', 'w+');
	usr_email = open('intermediate/multi_users_email.csv', 'w+');
	usr_http = open('intermediate/multi_users_http.csv', 'w+');
	multiUserExtractHelper('device')
	multiUserExtractHelper('logon')
	multiUserExtractHelper('file')
	multiUserExtractHelper('email')
	multiUserExtractHelper('http')

#######################
# COMBINE
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

def combine():
	fileList = [open('intermediate/multi_users_device.csv'),\
				open('intermediate/multi_users_email.csv'),\
				open('intermediate/multi_users_file.csv'),\
				open('intermediate/multi_users_http.csv'),\
				open('intermediate/multi_users_logon.csv')]
	outfile = open('intermediate/multi_users_aggregated.csv','w')
	combineFiles(fileList,outfile)
	for f in fileList:
		f.close()
	outfile.close()


############################
# ANNOTATE
# converts a timestamp to a string
def tsToStr(timestamp):
    return str(timestamp.date())+'T'+str(timestamp.time())

def logon(record,outfile):
    id = record[1][1:len(record[1])-1]
    timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
    print >>outfile, '%s%s-event %shasAction %slogon_%s . %s' %(ex,record[3],ex,ex,id,tsToStr(timestamp))
    print >>outfile, '%slogon_%s %s %s%sAction .' %(ex,id,a,ex,record[5])
    #print >>outfile, '%slogon_%s %shasTimestamp> "%s-05:00"^^<%sdateTime> . %s' %(ex,id,ex,tsToStr(timestamp),xsd,tsToStr(timestamp))
    if timestamp.time() < dailyStartDic[record[3]] or timestamp.time() > dailyEndDic[record[3]]:
        print >>outfile, '%slogon_%s %s %sAfterHourAction .' %(ex,id,a,ex)
    else:
        print >>outfile, '%slogon_%s %s %sInHourAction .' %(ex,id,a,ex)
    print >>outfile, '%slogon_%s %shasActor %s%s .' %(ex,id,ex,ex,record[3])
    print >>outfile, '%slogon_%s %sisPerformedOnPC %s%s .' %(ex,id,ex,ex,record[4])

def device(record,outfile):
    id = record[1][1:len(record[1])-1]
    timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
    print >>outfile, '%s%s-event %shasAction %sdevice_%s . %s' %(ex,record[3],ex,ex,id,tsToStr(timestamp))
    print >>outfile, '%sdevice_%s %s %s%sAction .' %(ex,id,a,ex,'Disk'+record[6]+'ion')
    #print >>outfile, '%sdevice_%s %shasTimestamp> "%s-05:00"^^<%sdateTime> . %s' %(ex,id,ex,tsToStr(timestamp),xsd,tsToStr(timestamp))
    if timestamp.time() < dailyStartDic[record[3]] or timestamp.time() > dailyEndDic[record[3]]:
        print >>outfile, '%sdevice_%s %s %sAfterHourAction .' %(ex,id,a,ex)
    else:
        print >>outfile, '%sdevice_%s %s %sInHourAction .' %(ex,id,a,ex)
    print >>outfile, '%sdevice_%s %shasActor %s%s .' %(ex,id,ex,ex,record[3])
    print >>outfile, '%sdevice_%s %sisPerformedOnPC %s%s .' %(ex,id,ex,ex,record[4])
    if (record[6]=='Connect'):
        print >>outfile, '%sdevice_%s %sisPerformedWithRemovableDisk %sdevice_%s_disk .' %(ex,id,ex,ex,id)
        print >>outfile, '%sdevice_%s_disk %shasFileTree> "%s" .' %(ex,id,ex,record[5].replace('\\','_'))

# email, id, date, user, pc, to, cc, bcc, from, activity, size, attachment, content
#   0,    1,   2,    3,   4,  5,  6,  7,    8,       9,    10,      11
def email(record,outfile):
    content = record[-1].replace(' ', '_');
    id = record[1][1:len(record[1])-1]
    timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
    print >>outfile, '%s%s-event %shasAction %semail_%s . %s' %(ex,record[3],ex,ex,id,tsToStr(timestamp))
    print >>outfile, '%semail_%s %s %sEmail%sAction .' %(ex,id,a,ex,record[9])
    #print >>outfile, '%semail_%s %shasTimestamp> "%s-05:00"^^<%sdateTime> . %s' %(ex,id,ex,tsToStr(timestamp),xsd,tsToStr(timestamp))
    if timestamp.time() < dailyStartDic[record[3]] or timestamp.time() > dailyEndDic[record[3]]:
        print >>outfile, '%semail_%s %s %sAfterHourAction .' %(ex,id,a,ex)
    else:
        print >>outfile, '%semail_%s %s %sInHourAction .' %(ex,id,a,ex)
    print >>outfile, '%semail_%s %shasActor %s%s .' %(ex,id,ex,ex,record[3])
    print >>outfile, '%semail_%s %sisPerformedOnPC %s%s .' %(ex,id,ex,ex,record[4])
    if record[5]:
        toList = record[5].split(';')
        for item in toList:
            print >>outfile, '%semail_%s %sto %s%s .'%(ex,id,ex,ex,item)
            if item[item.find('@'):] == '@dtaa.com':
                print >>outfile, '%semail_%s %s %sInternalEmailAddress .' %(ex,item,a,ex)
            else:
                print >>outfile, '%semail_%s %s %sNotInternalEmailAddress .' %(ex,item,a,ex)
    if record[6]:
        ccList = record[6].split(';')
        for item in ccList:
            print >>outfile, '%semail_%s %scc %s%s .'%(ex,id,ex,ex,item)
            if item[item.find('@'):] == '@dtaa.com':
                print >>outfile, '%semail_%s %s %sInternalEmailAddress .' %(ex,item,a,ex)
            else:
                print >>outfile, '%semail_%s %s %sNotInternalEmailAddress .' %(ex,item,a,ex)
    if record[7]:
        bccList = record[7].split(';')
        for item in bccList:
            print >>outfile, '%semail_%s %sbcc %s%s .'%(ex,id,ex,ex,item)
            if item[item.find('@'):] == '@dtaa.com':
                print >>outfile, '%semail_%s %s %sInternalEmailAddress .' %(ex,item,a,ex)
            else:
                print >>outfile, '%semail_%s %s %sNotInternalEmailAddress .' %(ex,item,a,ex)

    print >>outfile, '%semail_%s %sfrom %s%s .'%(ex,id,ex,ex,record[8])
    if record[8][record[8].find('@'):] == '@dtaa.com':
        print >>outfile, '%semail_%s %s %sInternalEmailAddress .' %(ex,record[8],a,ex)
    else:
        print >>outfile, '%semail_%s %s %sNotInternalEmailAddress .' %(ex,record[8],a,ex)

    print >>outfile, '%semail_%s %shasEmailSize> "%s bytes" .'%(ex,id,ex,record[10])
    if record[11]:
        attachmentList = record[11].split(';')
        for i in range(len(attachmentList)):
            filename = attachmentList[i][:attachmentList[i].find('(')].replace('\\','_')
            attachmentSize = attachmentList[i][attachmentList[i].find('(')+1:item.find(')')]
            print >>outfile, '%semail_%s %shasEmailAttachment %s%s .'%(ex,id,ex,ex,filename)
            print >>outfile, '%sattachment%s %shasSize> "%s bytes" .'%(ex,str(i+1),ex,attachmentSize)
    print >>outfile, '%semail_%s %shasContent> "%s" .'%(ex,id,ex,content)

# file, id, date, user, pc, filename, activity, to_removable_media, from_removable_media, content
#    0,  1,   2,   3,   4,    5,         6,               7,                8,               9
def file(record,outfile):
    content = record[-1].replace(' ','_')
    id = record[1][1:len(record[1])-1]
    timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
    print >>outfile, '%s%s-event %shasAction %sfile_%s . %s' %(ex,record[3],ex,ex,id,tsToStr(timestamp))
    print >>outfile, '%sfile_%s %s %sFile%sAction .' %(ex,id,a,ex,record[6][5:])
    #print >>outfile, '%sfile_%s %shasTimestamp> "%s-05:00"^^<%sdateTime> . %s' %(ex,id,ex,tsToStr(timestamp),xsd,tsToStr(timestamp))
    if timestamp.time() < dailyStartDic[record[3]] or timestamp.time() > dailyEndDic[record[3]]:
        print >>outfile, '%sfile_%s %s %sAfterHourAction .' %(ex,id,a,ex)
    else:
        print >>outfile, '%sfile_%s %s %sInHourAction .' %(ex,id,a,ex)
    print >>outfile, '%sfile_%s %shasActor %s%s .' %(ex,id,ex,ex,record[3])
    print >>outfile, '%sfile_%s %sisPerformedOnPC %s%s .' %(ex,id,ex,ex,record[4])
    filename = record[5].replace('\\','_')
    print >>outfile, '%sfile_%s %shasFile %s%s .'%(ex,id,ex,ex,filename)
    if record[7]=='True':
        print >>outfile, '%sfile_%s_file %s %sFileToRemovableMedia .' %(ex,id,a,ex)
    else:
        print >>outfile, '%sfile_%s_file %s %sNotFileToRemovableMedia .' %(ex,id,a,ex)
    if record[8]=='True':
        print >>outfile, '%sfile_%s_file %s %sFileFromRemovableMedia .' %(ex,id,a,ex)
    else:
        print >>outfile, '%sfile_%s_file %s %sNotFileFromRemovableMedia .' %(ex,id,a,ex)
    print >>outfile, '%sfile_%s_file %shasContent> "%s" .' %(ex,id,ex,content)

# http, id, date, user, pc, url, activity, content
#  0,    1,   2,   3,   4,   5,     6,        7
def http(record,outfile):
    content = record[-1].replace(' ', '_')
    id = record[1][1:len(record[1])-1]
    timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
    print >>outfile, '%s%s-event %shasAction %shttp_%s . %s' %(ex,record[3],ex,ex,id,tsToStr(timestamp))
    print >>outfile, '%shttp_%s %s %s%sAction .' %(ex,id,a,ex,record[6].replace(' ',''))
    #print >>outfile, '%shttp_%s %shasTimestamp> "%s-05:00"^^<%sdateTime> . %s' %(ex,id,ex,tsToStr(timestamp),xsd,tsToStr(timestamp))
    if timestamp.time() < dailyStartDic[record[3]] or timestamp.time() > dailyEndDic[record[3]]:
        print >>outfile, '%shttp_%s %s %sAfterHourAction .' %(ex,id,a,ex)
    else:
        print >>outfile, '%shttp_%s %s %sInHourAction .' %(ex,id,a,ex)
    print >>outfile, '%shttp_%s %shasActor %s%s .' %(ex,id,ex,ex,record[3])
    print >>outfile, '%shttp_%s %sisPerformedOnPC %s%s .' %(ex,id,ex,ex,record[4])
    print >>outfile, '%shttp_%s %shasURL %s .' %(ex,id,ex,record[5])
    domainName = record[5][:record[5].find('/',7)+1]
    if domainName in cloudStorageWebsites:
        print >>outfile, '%s %swhoseDomainNameIsA %scloudstoragewebsite .' %(record[5],ex,ex)
    elif domainName in hacktivistWebsites:
        print >>outfile, '%s %swhoseDomainNameIsA %shacktivistwebsite .' %(record[5],ex,ex)
    elif domainName in jobHuntingWebsites:
        print >>outfile, '%s %swhoseDomainNameIsA %sjobhuntingwebsite .' %(record[5],ex,ex)
    else:
        print >>outfile, '%s %s %sneuturalwebsite .' %(record[5],a,ex)
    print >>outfile, '%shttp_%s %shasContent> "%s" .' %(ex,id,ex,content)


def multiUsersAnnotate():
	f = open('intermediate/multi_users_aggregated.csv')
	outfile = open('multi_users_annotation.txt','w')
	# userID = f.readline().split(',')[3]
	# print >>outfile, '%s%s %sisInvolvedIn %s%s-event .' %(ex,userID,ex,ex,userID)
	# f.seek(0,0)
	# deviceUsageCounter = 0
	# isDeviceConnected = False
	# connectedDevice = ''
	for record in f:
	    type = record[:record.find(',')]
	    if type == 'logon':
	        record = record.strip().split(',')
	        # if record[5] == 'Logoff':
	            # connectedDevice = ''
	        logon(record,outfile)
	    elif type == 'device':
	        record = record.strip().split(',')
	        # if record[6] == 'Connect':
	            # deviceUsageCounter += 1
	            # connectedDevice = record[1][1:len(record[1])-1]  # id of this record
	        # elif record[6] == 'Disconnect':
	            # connectedDevice = ''
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
	        #if connectedDevice:
	        #    id = record[1][1:len(record[1])-1]
	        #    timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
            #    print >>outfile, '%s%s %sstartsNoEarlierThanEndingOf %s%s .|%s' %(ex,id,ex,ex,connectedDevice,tsToStr(timestamp))
	    elif type == 'http':
	        content = record[record.find('"'):len(record)-1].replace('"','')
	        record = record[:record.find('"')].split(',')
	        record.append(content)
	        http(record,outfile)
	    else:
	        print >>outfile, 'unknown type:', type
	        raise

	# if deviceUsageCounter > usbDriveUsageFrequency[user]:
	    # timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
	    # print >>outfile, '%s%s %s %sExcessiveRemovableDriveUser .' %(ex,userID,a,ex)
        #print >>outfile, '%s%s %s %sExcessiveRemovableDriveUser .|%s' %(ex,userID,a,ex,tsToStr(timestamp))
	f.close()
	outfile.close()
