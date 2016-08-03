# multiUsers.py
import sys, datetime
from urlparse import urlparse
from globals import *
path = '../data-r6.2/'
file_name = sys.argv[1]
subPath = '1-ACM2278/'
#subPath = '2-CMP2946/'
#subPath = '4-CDE1846/'
#subPath = '5-MBG3183/'

# returns a tuple (AverageLogonTime, AverageLogoffTime) where the two elements are dictionaries
# with keys being the userids, values being the average logon/logoff time of that user
def getRoutineHours(userList):
    def allDone(dic):
        for userID in userList:
            if userID not in dic.keys():
                return False
            if len(dic[userID])<10:
                return False
        return True

    def averageTime(timeList):
        avg = sum(ts.hour*3600 + ts.minute*60 + ts.second for ts in timeList)/len(timeList)
        minutes, seconds = divmod(int(avg), 60)
        hours, minutes = divmod(minutes, 60)
        r = datetime.datetime(1900,1,1,hours,minutes,seconds) # + datetime.timedelta(minutes=15)
        return r

    def getAverageLogon():
        logonTimesDic = {}
        with open('../data-r6.2/logon.csv') as inFile:
            while not allDone(logonTimesDic):
                line = inFile.readline()
                if not line:
                    print 'File ended before completing.'
                    # for u in logonTimesDic.keys():
                    #     print u, len(logonTimesDic[u])
                    inFile.close()
                    return

                line = line.strip().split(',')
                userID = line[2]
                # print line
                if userID in userList and line[4] == 'Logon':
                    ts = datetime.datetime.strptime(line[1],'%m/%d/%Y %H:%M:%S')
                    if userID in logonTimesDic.keys():
                        if len(logonTimesDic[userID]) < 10:
                            if ts.date() in logonTimesDic[userID].keys():
                                pass
                            else:
                                logonTimesDic[userID][ts.date()] = ts.time()
                    # the user does not exist in the dic
                    else:
                        logonTimesDic[userID] = {}
                        logonTimesDic[userID][ts.date()] = ts.time()
            # calculate the average for each userID
            for userID in logonTimesDic.keys():
                logonTimesDic[userID] = (averageTime(logonTimesDic[userID].values()) \
                                        + datetime.timedelta(minutes=-15)).time()
        return logonTimesDic

    def getAverageLogoff():
        logoffTimesDic = {}
        with open('../data-r6.2/logon.csv') as inFile:
            while not allDone(logoffTimesDic):
                line = inFile.readline()
                if not line:
                    print 'File ended before completing.'
                    # for u in logoffTimesDic.keys():
                    #     print u, len(logoffTimesDic[u])
                    inFile.close()
                    return

                line = line.strip().split(',')
                userID = line[2]
                # print line
                if userID in userList and line[4] == 'Logoff':
                    ts = datetime.datetime.strptime(line[1],'%m/%d/%Y %H:%M:%S')
                    if userID in logoffTimesDic.keys():
                        if len(logoffTimesDic[userID]) < 10:
                            logoffTimesDic[userID][ts.date()] = ts.time()
                    # the user does not exist in the dic
                    else:
                        logoffTimesDic[userID] = {}
                        logoffTimesDic[userID][ts.date()] = ts.time()
            # calculate the average for each userID
            for userID in logoffTimesDic.keys():
                logoffTimesDic[userID] = (averageTime(logoffTimesDic[userID].values())\
                                        + datetime.timedelta(minutes=15)).time()
        return logoffTimesDic

    return getAverageLogon(),getAverageLogoff()


def getAverageUSBUsage(userList):
    def allDone(dic):
        for userID in userList:
            if len(dic[userID])<10:
                return False
        return True

    usageDic = dict((userID,{}) for userID in userList)
    # get 10 logon days for each user
    with open('../data-r6.2/logon.csv') as logonFile:
        while not allDone(usageDic):
            line = logonFile.readline()
            if not line:
                print 'File ended before completing.'
                # for u in logoffTimesDic.keys():
                #     print u, len(logoffTimesDic[u])
                logonFile.close()
                return
            line = line.strip().split(',')
            userID = line[2]
            if userID in userList and len(usageDic[userID]) < 10:
                # usageDic[userID][date] = count
                ts = datetime.datetime.strptime(line[1],'%m/%d/%Y %H:%M:%S')
                usageDic[userID][ts.date()] = 0

    maxdate = max([max(usageDic[userID].keys()) for userID in userList])
    # count number of device connect for each day
    with open('../data-r6.2/device.csv') as deviceFile:
        deviceFile.readline()
        for line in deviceFile:
            # print line
            line = line.strip().split(',')
            ts = datetime.datetime.strptime(line[1],'%m/%d/%Y %H:%M:%S')
            if ts.date() > maxdate:
                break
            userID = line[2]
            # print line[5]
            if userID in userList and line[5] == 'Connect' and ts.date() in usageDic[userID].keys():
                usageDic[userID][ts.date()] += 1
                # print userID, ts.date()

    # calculate the average
    for userID in usageDic.keys():
        usageDic[userID] = sum(usageDic[userID].values())/10.0
    return usageDic


#######################
# EXTRACT
def multiUserExtract(userList):
	def multiUserExtractHelper(actionType):
		# inFile = open(path+actionType+'.csv')
		inFile = open(path+subPath+actionType+'.csv')
		outFile = open('intermediate/multi_users_'+actionType+'.csv', 'w')
		for line in inFile:
			if line.split(',')[2] in userList:
				outFile.write(actionType+','+line)
		outFile.close()
		inFile.close()
		print actionType, 'extract done.'

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


def multiUserCombine():
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


def multiUserAnnotate(userList, dailyStartDic, dailyEndDic, usbDriveUsageFrequency):
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

	def device(record,outfile,deviceUsageCounter):
		id = record[1][1:len(record[1])-1]
		userID = record[3]
		timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
		print >>outfile, '%s%s-event %shasAction %sdevice_%s . %s' %(ex,record[3],ex,ex,id,tsToStr(timestamp))
		if deviceUsageCounter > usbDriveUsageFrequency[userID]:
			timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
			print >>outfile, '%s%s %s %sExcessiveRemovableDriveUser .' %(ex,userID,a,ex)
			#print >>outfile, '%s%s %s %sExcessiveRemovableDriveUser .|%s' %(ex,userID,a,ex,tsToStr(timestamp))
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
	#	0,	1,	2,	3,	4,  5,  6,  7,	8,		9,	10,		11
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
						print >>outfile, '%s%s %s %sInternalEmailAddress .' %(ex,item,a,ex)
					else:
						print >>outfile, '%s%s %s %sNotInternalEmailAddress .' %(ex,item,a,ex)
		if record[6]:
			ccList = record[6].split(';')
			for item in ccList:
					print >>outfile, '%semail_%s %scc %s%s .'%(ex,id,ex,ex,item)
					if item[item.find('@'):] == '@dtaa.com':
						print >>outfile, '%s%s %s %sInternalEmailAddress .' %(ex,item,a,ex)
					else:
						print >>outfile, '%s%s %s %sNotInternalEmailAddress .' %(ex,item,a,ex)
		if record[7]:
			bccList = record[7].split(';')
			for item in bccList:
					print >>outfile, '%semail_%s %sbcc %s%s .'%(ex,id,ex,ex,item)
					if item[item.find('@'):] == '@dtaa.com':
						print >>outfile, '%s%s %s %sInternalEmailAddress .' %(ex,item,a,ex)
					else:
						print >>outfile, '%s%s %s %sNotInternalEmailAddress .' %(ex,item,a,ex)

		print >>outfile, '%semail_%s %sfrom %s%s .'%(ex,id,ex,ex,record[8])
		if record[8][record[8].find('@'):] == '@dtaa.com':
			print >>outfile, '%s%s %s %sInternalEmailAddress .' %(ex,record[8],a,ex)
		else:
			print >>outfile, '%s%s %s %sNotInternalEmailAddress .' %(ex,record[8],a,ex)

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
	#	0,  1,	2,	3,	4,	5,			6,					7,					8,					9
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
			print >>outfile, '%s%s %s %sFileToRemovableMedia .' %(ex,filename,a,ex)
		else:
			print >>outfile, '%s%s %s %sNotFileToRemovableMedia .' %(ex,filename,a,ex)
		if record[8]=='True':
			print >>outfile, '%s%s %s %sFileFromRemovableMedia .' %(ex,filename,a,ex)
		else:
			print >>outfile, '%s%s %s %sNotFileFromRemovableMedia .' %(ex,filename,a,ex)
		print >>outfile, '%sfile_%s_file %shasContent> "%s" .' %(ex,id,ex,content)

	# http, id, date, user, pc, url, activity, content
	#  0,	1,	2,	3,	4,	5,	6,		7
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
		parsedURL = urlparse(record[5])
		domainName = parsedURL.scheme + '://' + parsedURL.netloc + '/'
		if domainName in cloudStorageWebsites:
			print >>outfile, '%s %swhoseDomainNameIsA %scloudstoragewebsite .' %(record[5],ex,ex)
		elif domainName in hacktivistWebsites:
			print >>outfile, '%s %swhoseDomainNameIsA %shacktivistwebsite .' %(record[5],ex,ex)
		elif domainName in jobHuntingWebsites:
			print >>outfile, '%s %swhoseDomainNameIsA %sjobhuntingwebsite .' %(record[5],ex,ex)
		else:
			if '/jobs/' in parsedURL.path or '/hotjobs/' in parsedURL.path:
				print >>outfile, '%s %swhoseDomainNameIsA %sjobhuntingwebsite .' %(record[5],ex,ex)
			else:
				print >>outfile, '%s %swhoseDomainNameIsA %sneutralwebsite .' %(record[5],ex,ex)
		print >>outfile, '%shttp_%s %shasContent> "%s" .' %(ex,id,ex,content)



	f = open('intermediate/multi_users_aggregated.csv')
	outfile = open(file_name + '_annotation.txt','w')
	# make a counter dictionary with key = userid, value = (deviceUsageCount, connectedDevice)
	deviceDic = {}
	for userID in userList:
		deviceDic[userID] = {'count':0, 'connectedDevice':''}
		print >>outfile, '%s%s %sisInvolvedIn %s%s-event .' %(ex,userID,ex,ex,userID)

	currentTS = '' # current timestamp
	for record in f:
		type = record[:record.find(',')]
		if type == 'logon':
			record = record.strip().split(',')
			if record[5] == 'Logoff':
				userID = record[3]
				deviceDic[userID]['connectedDevice'] = ''
			logon(record,outfile)
		elif type == 'device':
			# set the count to zero each day
			record = record.strip().split(',')
			userID = record[3]

			if record[2][:record[2].find(' ')] != currentTS:
				currentTS = record[2][:record[2].find(' ')]
				for x in deviceDic.keys():
					deviceDic[x]['count'] = 0

			if record[6] == 'Connect':
				deviceDic[userID]['count'] += 1
				deviceDic[userID]['connectedDevice'] = record[1][1:len(record[1])-1]  # id of this record
			elif record[6] == 'Disconnect':
				deviceDic[userID]['connectedDevice'] = ''
			device(record,outfile,deviceDic[userID]['count'])
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

			userID = record[3]
			if deviceDic[userID]['connectedDevice']:
				id = record[1][1:len(record[1])-1]
				timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
				print >>outfile, '%sfile_%s %sstartsNoEarlierThanEndingOf %sdevice_%s .' \
									%(ex,id,ex,ex,deviceDic[userID]['connectedDevice'])
		elif type == 'http':
			content = record[record.find('"'):len(record)-1].replace('"','')
			record = record[:record.find('"')].split(',')
			record.append(content)
			http(record,outfile)
		else:
			print >>outfile, 'unknown type:', type
			raise

	f.close()
	outfile.close()

if __name__ == '__main__':

	if len(sys.argv)<2:
		print 'USAGE: python multiUsers.py [filename]'
		sys.exit(0)

	infile = open(sys.argv[1])
	userList = [line.strip() for line in infile]
	infile.close()

	dailyStartDic, dailyEndDic = getRoutineHours(userList)
	usbDriveUsageFrequency = getAverageUSBUsage(userList)
	multiUserExtract(userList)
	print 'Extract done.'
	multiUserCombine()
	print 'Combine done.'
	multiUserAnnotate(userList, dailyStartDic, dailyEndDic, usbDriveUsageFrequency)
	print 'Annotate done.'
