# groundTruth.py
import sys, datetime

def tsToStr(timestamp):
    return str(timestamp.date())+'T'+str(timestamp.time())

def groundTruth(filename):
    inFile = open(filename)
    line = inFile.readline().split(',')
    userID = line[3]
    outFile = open(userID+'.txt', 'w')
    # outFile.write(line[0]+'_'+line[1][1:len(line[1])-1]+'\n')
    inFile.seek(0)
    for line in inFile:
        line = line.split(',')
        actionID = line[0]+'_'+line[1][1:len(line[1])-1]
        timestamp = tsToStr(datetime.datetime.strptime(line[2],'%m/%d/%Y %H:%M:%S'))+'-05:00'
        userID = line[3]
        outFile.write(','.join([actionID,timestamp,userID])+'\n')
    inFile.close()
    outFile.close()

if __name__ == '__main__':
    path = '../data-r6.2/answers/'
    filenames = [path+'r6.2-1.csv',path+'r6.2-2.csv',path+'r6.2-4.csv',path+'r6.2-5.csv']
    for f in filenames:
        groundTruth(f)
