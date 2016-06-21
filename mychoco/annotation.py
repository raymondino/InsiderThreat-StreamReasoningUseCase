# Data annotation & ontology construction
import datetime
from globals import *
dailyStart = datetime.time(7,27-15,12)
dailyEnd = datetime.time(17,34+15,48)

def logon(record):
    record = record.strip().split(',')
    id = record[1][1:len(record[1])-1]
    timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
    print '<%slogon_%s> <%s> <%s%sAction>.' %(ex,id,a,ex,record[5])
    print '<%slogon_%s> <%shasTimestamp> "%s-05:00"^^<%sdateTimeStamp>.' %(ex,id,ex,str(timestamp.date())+'T'+str(timestamp.time()),xsd )
    if timestamp.time() < dailyStart or timestamp.time() > dailyEnd:
        print '<%slogon_%s> <%s> <%sAfterHourAction>.' %(ex,id,a,ex)
    else:
        print '<%slogon_%s> <%s> <%sInHourAction>.' %(ex,id,a,ex)
    print '<%slogon_%s> <%shasActor> <%s%s>.' %(ex,id,ex,ex,record[3])
    print '<%slogon_%s> <%sisPerformedOnPC> <%s%s>.' %(ex,id,ex,ex,record[4])

def device(record):
    record = record.strip().split(',')
    id = record[1][1:len(record[1])-1]
    timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
    print '<%sdevice_%s> <%s> <%s%sAction>.' %(ex,id,a,ex,record[6]+'ion')
    print '<%sdevice_%s> <%shasTimestamp> "%s-05:00"^^<%sdateTimeStamp>.' %(ex,id,ex,str(timestamp.date())+'T'+str(timestamp.time()),xsd )
    if timestamp.time() < dailyStart or timestamp.time() > dailyEnd:
        print '<%sdevice_%s> <%s> <%sAfterHourAction>.' %(ex,id,a,ex)
    else:
        print '<%sdevice_%s> <%s> <%sInHourAction>.' %(ex,id,a,ex)
    print '<%sdevice_%s> <%shasActor> <%s%s>.' %(ex,id,ex,ex,record[3])
    print '<%sdevice_%s> <%sisPerformedOnPC> <%s%s>.' %(ex,id,ex,ex,record[4])
    if (record[6]=='Connect'):
        print '<%sdevice_%s> <%sisPerformedWithRemovableDisk> <%sdevice_%s_disk>.' %(ex,id,ex,ex,id)
        print '<%sdevice_%s_disk> <%shasFileTree> "%s"^^<%sstring>.' %(ex,id,ex,record[5].replace('\\','_'),xsd)

# email, id, date, user, pc, to, cc, bcc, from, activity, size, attachment, content
def email(record):
    content = record[record.find('"'):len(record)-1].replace('"','')
    record = record[:record.find('"')].split(',')
    id = record[1][1:len(record[1])-1]
    timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
    print '<%semail_%s> <%s> <%sEmail%sAction>.' %(ex,id,a,ex,record[9])
    print '<%semail_%s> <%shasTimestamp> "%s-05:00"^^<%sdateTimeStamp>.' %(ex,id,ex,str(timestamp.date())+'T'+str(timestamp.time()),xsd )
    if timestamp.time() < dailyStart or timestamp.time() > dailyEnd:
        print '<%semail_%s> <%s> <%sAfterHourAction>.' %(ex,id,a,ex)
    else:
        print '<%semail_%s> <%s> <%sInHourAction>.' %(ex,id,a,ex)
    print '<%semail_%s> <%shasActor> <%s%s>.' %(ex,id,ex,ex,record[3])
    print '<%semail_%s> <%sisPerformedOnPC> <%s%s>.' %(ex,id,ex,ex,record[4])
    if record[5]:
        toList = record[5].split(';')
        for item in toList:
            print '<%semail_%s> <%sto> <%s%s>.'%(ex,id,ex,ex,item)
    if record[6]:
        ccList = record[6].split(';')
        for item in ccList:
            print '<%semail_%s> <%scc> <%s%s>.'%(ex,id,ex,ex,item)
    if record[7]:
        bccList = record[7].split(';')
        for item in bccList:
            print '<%semail_%s> <%sbcc> <%s%s>.'%(ex,id,ex,ex,item)
    print '<%semail_%s> <%sfrom> <%s%s>.'%(ex,id,ex,ex,record[8])
    print '<%semail_%s> <%shasEmailSize> "%s bytes"^^<%sstring>.'%(ex,id,ex,record[10],xsd)
    if record[11]:
        attachmentList = record[11].split(';')
        for i in range(len(attachmentList)):
            filename = attachmentList[i][:attachmentList[i].find('(')].replace('\\','_')
            attachmentSize = attachmentList[i][attachmentList[i].find('(')+1:item.find(')')]
            print '<%semail_%s> <%shasEmailAttachment> <%s%s>.'%(ex,id,ex,ex,filename)
            print '<%sattachment%s> <%shasSize> "%s bytes"^^<%sstring>.'%(ex,str(i+1),ex,attachmentSize,xsd)
    print '<%semail_%s> <%shasContent> "%s"^^<%sstring>.'%(ex,id,ex,content,xsd)

# file, id, date, user, pc, filename, activity, to_removable_media, from_removable_media, content
#    0,  1,   2,   3,   4,    5,         6,               7,                8,               9
def file(record):
    content = record[record.find('"'):len(record)-1].replace('"','')
    record = record[:record.find('"')].split(',')
    id = record[1][1:len(record[1])-1]
    timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
    print '<%sfile_%s> <%s> <%sFile%sAction>.' %(ex,id,a,ex,record[6][5:])
    print '<%sfile_%s> <%shasTimestamp> "%s-05:00"^^<%sdateTimeStamp>.' %(ex,id,ex,str(timestamp.date())+'T'+str(timestamp.time()),xsd )
    if timestamp.time() < dailyStart or timestamp.time() > dailyEnd:
        print '<%sfile_%s> <%s> <%sAfterHourAction>.' %(ex,id,a,ex)
    else:
        print '<%sfile_%s> <%s> <%sInHourAction>.' %(ex,id,a,ex)
    print '<%sfile_%s> <%shasActor> <%s%s>.' %(ex,id,ex,ex,record[3])
    print '<%sfile_%s> <%sisPerformedOnPC> <%s%s>.' %(ex,id,ex,ex,record[4])
    filename = record[5].replace('\\','_')
    print '<%sfile_%s> <%shasFile> <%s%s>.'%(ex,id,ex,ex,filename)
    if record[7]=='True':
        print '<%sfile_%s_file> <%s> <%sFileToRemovableMedia>.' %(ex,id,a,ex)
    else:
        print '<%sfile_%s_file> <%s> <%sNotFileToRemovableMedia>.' %(ex,id,a,ex)
    if record[8]=='True':
        print '<%sfile_%s_file> <%s> <%sFileFromRemovableMedia>.' %(ex,id,a,ex)
    else:
        print '<%sfile_%s_file> <%s> <%sNotFileFromRemovableMedia>.' %(ex,id,a,ex)
    print '<%sfile_%s_file> <%shasContent> "%s"^^<%sstring>.' %(ex,id,ex,content,xsd)

# http, id, date, user, pc, url, activity, content
#  0,    1,   2,   3,   4,   5,     6,        7
def http(record):
    content = record[record.find('"'):len(record)-1].replace('"','')
    record = record[:record.find('"')].split(',')
    id = record[1][1:len(record[1])-1]
    timestamp = datetime.datetime.strptime(record[2],'%m/%d/%Y %H:%M:%S')
    print '<%shttp_%s> <%s> <%s%sAction>.' %(ex,id,a,ex,record[6].replace(' ',''))
    print '<%shttp_%s> <%shasTimestamp> "%s-05:00"^^<%sdateTimeStamp>.' %(ex,id,ex,str(timestamp.date())+'T'+str(timestamp.time()),xsd )
    if timestamp.time() < dailyStart or timestamp.time() > dailyEnd:
        print '<%shttp_%s> <%s> <%sAfterHourAction>.' %(ex,id,a,ex)
    else:
        print '<%shttp_%s> <%s> <%sInHourAction>.' %(ex,id,a,ex)
    print '<%shttp_%s> <%shasActor> <%s%s>.' %(ex,id,ex,ex,record[3])
    print '<%shttp_%s> <%sisPerformedOnPC> <%s%s>.' %(ex,id,ex,ex,record[4])
    print '<%shttp_%s> <%shasURL> <%s>.' %(ex,id,ex,record[5])
    domainName = record[5][:record[5].find('/',7)+1]
    if domainName in cloudStorageWebsites:
        print '<%s> <%s> <%sCloudStorageWebsite>.' %(record[5],ex'whoseDomainNameIsA',ex)
    elif domainName in hacktivistWebsites:
        print '<%s> <%s> <%sHacktivistWebsite>.' %(record[5],ex'whoseDomainNameIsA',ex)
    elif domainName in jobHuntingWebsites:
        print '<%s> <%s> <%sJobHuntingWebsite>.' %(record[5],ex'whoseDomainNameIsA',ex)
    else:
        print '<%s> <%s> <%sNeuturalWebsite>.' %(record[5],a,ex)
    print '<%shttp_%s> <%shasContent> "%s"^^<%sstring>.' %(ex,id,ex,content,xsd)





if __name__ == '__main__':
    f = open('ACM2278-aggregated.csv')
    for line in f:
        type = line[:line.find(',')]
        if type == 'logon':
            logon(line)
        elif type == 'device':
            device(line)
        elif type == 'email':
            email(line)
        elif type == 'file':
            file(line)
        elif type == 'http':
            http(line)
        else:
            print 'unknown type:', type


    f.close()
