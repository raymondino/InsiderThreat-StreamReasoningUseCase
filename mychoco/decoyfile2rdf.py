# decoyfile2rdf.py
from globals import *

def decoyToRDF(record):
    record = record.strip().split(',')
    filename = record[0].replace('\\','_')
    print '<%s%s> <%s> <%sDecoyFile>' %(ex,filename,a,ex)
    print '<%s%s> <%sisIn> <%s%s>' %(ex,filename,ex,ex,record[1])

if __name__ == '__main__':
    f = open('decoy_file.csv')
    f.readline()
    for line in f:
        decoyToRDF(line)
