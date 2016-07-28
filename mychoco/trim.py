# trim.py
import datetime
path = '../data-r6.2/'

def trim(filename,startDate, endDate):
    inFile = open(path+filename)
    outFile = open(path+filename[:filename.rfind('.')]+'_trimmed.csv','w')
    inFile.readline()
    for line in inFile:
        # t = line.split(',')[1]
        t = line[25:44]
        timestamp = datetime.datetime.strptime(t,'%m/%d/%Y %H:%M:%S')
        if timestamp.date() >= startDate:
            if timestamp.date() > endDate:
                break
            else:
                outFile.write(line)
    inFile.close()
    outFile.close()

def trimTo4(filename, startDate, endDate, userID):
    inFile = open(path+filename)
    outFile = open(path+userID+'/'+filename[:filename.rfind('.')]+'_'+userID+'.csv','w')
    inFile.readline()
    for line in inFile:
        # t = line.split(',')[1]
        t = line[25:44]
        timestamp = datetime.datetime.strptime(t,'%m/%d/%Y %H:%M:%S')
        if timestamp.date() >= startDate:
            if timestamp.date() > endDate:
                break
            else:
                outFile.write(line)
    inFile.close()
    outFile.close()

if __name__ == '__main__':
    fileList = ['logon.csv','device.csv','email.csv','file.csv','http.csv']
    # startDate = datetime.date(2010,8,17)
    # endDate = datetime.date(2011,4,26)
    userTimestampDic = {'ACM2278': (datetime.date(2010,8,18),datetime.date(2010,8,25)),
                        'CMP2946': (datetime.date(2011,2,2), datetime.date(2011,3,31)),
                        'CDE1846': (datetime.date(2011,2,21), datetime.date(2011,4,26)),
                        'MBG3183': (datetime.date(2010,10,12), datetime.date(2010,10,14))}

    for f in fileList:
        with open(path+f) as inFile:
            outFile1 = open(path+'ACM2278/'+f[:f.rfind('.')],'w')
            outFile2 = open(path+'CMP2946/'+f[:f.rfind('.')],'w')
            outFile3 = open(path+'CDE1846/'+f[:f.rfind('.')],'w')
            outFile4 = open(path+'MBG3183/'+f[:f.rfind('.')],'w')
            inFile.readline()
            for line in inFile:
                t = line[25:44]
                timestamp = datetime.datetime.strptime(t,'%m/%d/%Y %H:%M:%S')
                if timestamp.date() < datetime.date(2010,8,18):
                    continue
                if timestamp.date() >= datetime.date(2011,4,26):
                    break
                if timestamp.date() >= datetime.date(2010,8,18) and timestamp.date() < datetime.date(2010,8,25):
                    outFile1.write(line)
                if timestamp.date() >= datetime.date(2011,2,2) and timestamp.date() < datetime.date(2011,3,31):
                    outFile2.write(line)
                if timestamp.date() >= datetime.date(2011,2,21) and timestamp.date() < datetime.date(2011,4,26):
                    outFile3.write(line)
                if timestamp.date() >= datetime.date(2010,10,12) and timestamp.date() < datetime.date(2010,10,14):
                    outFile4.write(line)

            outFile1.close()
            outFile2.close()
            outFile3.close()
            outFile4.close()
        print f, 'finished.'
        inFile.close()
