# ldap2rdf.py
from globals import *
# employee_name,user_id,email,role,projects,business_unit,functional_unit,department,team,supervisor
#      0,          1,     2,   3,    4,         5,              6,            7,      8,    9
def ldapToRDF(record):
    record = record.strip().split(',')
    print '<%s%s> <%shasName> <%s%s>.' %(ex,record[1],ex,ex,record[0].replace(' ','_'))
    print '<%s%s> <%shasEmailAddress> <%s%s>.' %(ex,record[1],ex,ex,record[2])
    if record[3]:
        print '<%s%s> <%shasRole> <%s%s>.' %(ex,record[1],ex,ex,record[3])
    if record[4]:
        print '<%s%s> <%shasProjects> <%s%s>.' %(ex,record[1],ex,ex,record[4].replace(' ',''))
    if record[5]:
        print '<%s%s> <%shasBussinessUnit> "%s"^^<%sstring>.' %(ex,record[1],ex,record[5].replace(' ',''),xsd)
    if record[6]:
        print '<%s%s> <%shasFunctionalUnit> "%s"^^<%sstring>.' %(ex,record[1],ex,record[6].replace(' ',''),xsd)
    if record[7]:
        print '<%s%s> <%shasDepartment> "%s"^^<%sstring>.' %(ex,record[1],ex,record[7].replace(' ',''),xsd)
    if record[8]:
        print '<%s%s> <%shasTeam> "%s"^^<%sstring>.' %(ex,record[1],ex,record[8].replace(' ',''),xsd)
    if record[9]:
        print '<%s%s> <%shasSupervisor> <%s%s>.' %(ex,record[1],ex,ex,record[9].replace(' ','_'))

if __name__ == '__main__':
    f = open('/media/sida/TOSHIBA EXT/InsiderThreatData/r6.2/LDAP/2009-12.csv')
    f.readline()
    for line in f:
        ldapToRDF(line)
