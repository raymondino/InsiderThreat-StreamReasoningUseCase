import datetime
MAXTIME = datetime.datetime(9999, 12, 31, 23, 59, 59)

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

def combine(fileList,outFile):
    firstLines = []
    for eachFile in fileList:
        firstLines.append(eachFile.readline())
    while not allEmpty(firstLines):
        i= findEarliest(firstLines)
        outFile.write(firstLines[i])
        firstLines[i] = fileList[i].readline()

if __name__ == '__main__':
    user = 'ACM2278'
    fileList = [open(user+'_device.csv'),open(user+'_email.csv'),open(user+'_file.csv'),open(user+'_http.csv'),open(user+'_logon.csv')]
    outfile = open('test.csv','w')
    combine(fileList,outfile)
    for f in fileList:
        f.close()
    outfile.close()
