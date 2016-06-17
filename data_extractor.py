user = "CDE1846";
#ACM2278 - scenario 1
#CDE1846 - scenario 4
#MBG3183 - scenario 5


usr_device = open(user+"_device.csv", 'w+');
usr_email = open(user+"_email.csv", 'w+');
usr_file = open(user+"_file.csv", 'w+');
usr_http = open(user+"_http.csv", 'w+');
usr_logon = open(user+"_logon.csv", 'w+');

device = open('device.csv', 'r')
for d in device:
	if d.split(',')[2] == user:
		usr_device.write("device," + d);
device.close();
usr_device.close();

logon = open('logon.csv', 'r')
for l in logon:
	if l.split(',')[2] == user :
		usr_logon.write("logon," + l);
logon.close();
usr_logon.close();

file = open('file.csv', 'r')
for f in file:
	if f.split(',')[2] == user :
		usr_file.write("file," + f);
file.close();
usr_file.close();

email = open('email.csv', 'r')
for e in email:
	if e.split(',')[2] == user :
		usr_email.write("email," + e);
email.close();
usr_email.close();

http = open('http.csv', 'r')
for h in http:
	if h.split(',')[2] == user :
		usr_http.write("http," + h);
http.close();
usr_http.close();