# gtf1 = open('ACM2278.txt', 'r')
# gtf2 = open('bench_ws-P1D_u-10_ACM2278_noSI.txt', 'r')

# gtf1 = open('CDE1846.txt', 'r')
# gtf2 = open('bench_ws-P7D_u-10_CDE1846_prov-trust.txt', 'r')

gtf1 = open('CMP2946.txt', 'r')
gtf2 = open('bench_ws-P7D_u-10_CMP2946_prov-trust.txt', 'r')

# gtf1 = open('MBG3183.txt', 'r')
# gtf2 = open('bench_ws-P1D_u-10_MBG3183_noSI.txt', 'r')

gta = [];
for line in gtf1:
	gta.append(line.split(',')[0])

recall = len(gta)

correct = 0.0;
precision = 0.0;

for line in gtf2:
	precision += 1
	if line.split(',')[0] in gta:
		correct += 1
		gta.remove(line.split(',')[0])
	else:
		print 'false positive:' + line.split(',')[0]
for x in gta:
	print  'false negative:' + x

print('precision = ' + str(correct / precision))
print('recall = ' + str(correct / recall))