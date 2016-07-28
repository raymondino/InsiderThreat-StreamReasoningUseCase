# trim.py
import datetime
path = '../data-r6.2/'

if __name__ == '__main__':
    fileList = ['logon.csv','device.csv','email.csv','file.csv','http.csv']
    userTimestampDic = {'ACM2278': (datetime.date(2010,8,18),datetime.date(2010,8,25)),
                        'CMP2946': (datetime.date(2011,2,2), datetime.date(2011,3,31)),
                        'CDE1846': (datetime.date(2011,2,21), datetime.date(2011,4,26)),
                        'MBG3183': (datetime.date(2010,10,12), datetime.date(2010,10,14))}

    for f in fileList:
        with open(path+f) as inFile:
            outFile1 = open(path+'1-ACM2278/'+f[:f.rfind('.')]+'.csv','w')
            outFile2 = open(path+'2-CMP2946/'+f[:f.rfind('.')]+'.csv','w')
            outFile3 = open(path+'4-CDE1846/'+f[:f.rfind('.')]+'.csv','w')
            outFile4 = open(path+'5-MBG3183/'+f[:f.rfind('.')]+'.csv','w')
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
