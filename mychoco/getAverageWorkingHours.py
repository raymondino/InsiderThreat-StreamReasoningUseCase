# getAverageWorkingHours.py
import sys, datetime
from globals import *
path = '../data-r6.2/'

# Average of a user's logon times in the first 10 days -15min
# returns a datetime.time object
def getAverageLogon(user):
    inFile = open('intermediate/'+user+'_logon.csv')
    timeList = []
    i = 0
    yesterday = datetime.datetime(1900,1,1,0,0,0)
    while i < 10:
        record = inFile.readline().strip().split(',')
        ts = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
        if ts.date() != yesterday.date():
            timeList.append(ts)
            i += 1
        yesterday = ts
    avg = sum(ts.hour*3600 + ts.minute*60 + ts.second for ts in timeList)/10
    minutes, seconds = divmod(int(avg), 60)
    hours, minutes = divmod(minutes, 60)
    r = datetime.datetime(1900,1,1,hours,minutes,seconds) + datetime.timedelta(minutes=-15)
    return r.time()

# Average of a user's logoff times in the first 10 days +15min
# returns a datetime.time object
def getAverageLogoff(user):
    inFile = open('intermediate/'+user+'_logon.csv')
    timeList = []
    i = -1
    yesterday = datetime.datetime(1900,1,1,0,0,0)
    while i < 10:
        record = inFile.readline().strip().split(',')
        ts = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
        if ts.date() != yesterday.date():
            timeList.append(yesterday)
            i += 1
        yesterday = ts
    timeList.pop(0)
    avg = sum(ts.hour*3600 + ts.minute*60 + ts.second for ts in timeList)/10
    minutes, seconds = divmod(int(avg), 60)
    hours, minutes = divmod(minutes, 60)
    r = datetime.datetime(1900,1,1,hours,minutes,seconds) + datetime.timedelta(minutes=15)
    return r.time()


if __name__ == '__main__':
    print getAverageLogon('ACM2278')
    print getAverageLogoff('ACM2278')
