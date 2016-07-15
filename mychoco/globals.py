# globals.py
# containing global variables
import datetime
cloudStorageWebsites = ['http://www.4shared.com/','http://1and1.com/','https://archive.org/','http://bluehost.com/','http://bp.blogspot.com/','http://yousendit.com/','http://yfrog.com/','http://webs.com/','http://twitpic.com/','http://soundcloud.com/','http://secureserver.net/','http://custhelp.com/','http://megaupload.com/','http://megaclick.com/','http://hostgator.com/','http://flippa.com/','http://dropbox.com/']
hacktivistWebsites = ['http://wikileaks.com']
jobHuntingWebsites = ['http://careerbuilder.com/','http://craigslist.org/','http://simplyhired.com/','http://monster.com/','http://jobhuntersbible.com/','http://job-hunt.org/','http://indeed.com/','http://linkedin.com/']
ex = 'http://tw.rpi.edu/ontology/DataExfiltration/'
a = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'
xsd = 'http://www.w3.org/2001/XMLSchema#'

# Average logon time of first 10 days -15min
dailyStartDic = {'ACM2278': datetime.time(7,12,12), \
               'CMP2946': datetime.time(8,42,36), \
               'CDE1846': datetime.time(8,27,6), \
               'MBG3183': datetime.time(7,56,36) }
# Average logoff of first 10 days +15min
dailyEndDic = {'ACM2278': datetime.time(17,49,48), \
              'CMP2946': datetime.time(18,18,36), \
              'CDE1846': datetime.time(19,01,24), \
              'MBG3183': datetime.time(17,29,54) }

usbDriveUsageFrequency = {'ACM2278': 0, 'CMP2946': 10.2, 'PLJ1771': 0, 'CDE1846': 0, 'MBG3183': 3.7 }
