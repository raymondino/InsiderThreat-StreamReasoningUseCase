# InsiderTreatUseCase
This use case aims to leverage stream reasoning techniques and the concept of semantic importance to detect one attacking type of the insider threat -- data exfiltration

# Dataset
We use synthesized dataset -> [link](https://www.cert.org/insider-threat/tools/). 
We used r6.2 (dataset released by v6.2 generator)
A diagram shows the overall dataset -> [link](https://www.dropbox.com/s/95615yuownzztvi/insider%20threat%20data%20diagram.png?dl=0)

# Sample data files
ACM2278 is a user that is detected to be a malicious insider. ACM2278_device.csv records this user's removable disk usage. 

# Two python scripts:
data_extractor.py extracts ACM2278's data from the overall datasets. The overall datasets contains 4000 users activities of web browsing, uploading, removable disk usage, logon/logoff, file etc. 
data_combine.py combines all the extracted file into one single file called ACM2278-aggregated.csv. This file contains all the merged data that are sorted according to each data's timestamp. So ACM2278's activies are all contained in this single file, following a temporal order. 
