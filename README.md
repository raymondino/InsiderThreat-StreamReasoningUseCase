# Insider Threat Use Case
This use case aims to leverage stream reasoning techniques and the concept of semantic importance to detect one attacking type of the insider threat -- data exfiltration

# Dataset
We use synthesized [dataset](https://www.cert.org/insider-threat/tools/).
We used r6.2 (dataset released by v6.2 generator)
A [diagram](https://www.dropbox.com/s/95615yuownzztvi/insider%20threat%20data%20diagram.png?dl=0) shows the overall dataset
We only provide the link as the size is about 100GB.

# Ontology
There two ontology files in two different folders. 
**ontology-data exfiltration alone** contains the knowledge specifically designed for **data exfiltration**, which is identified as one of the many attacking types of the insider threat. This ontology extends the [original cert ontology](http://resources.sei.cmu.edu/library/asset-view.cfm?assetID=454613) which includes the class hierarchy of insider threat indicators. However, right now (7/14/2016), the ontology is not consistant. 

![alt text](http://i.imgur.com/RI3nffZ.png "CERT ontology inconsistency explanation")

*TradeSecretInformation* is reasoned to be both *Asset* and *Information*, while *Asset* and *Information* class are mutually disjoint. 

This problem can be technically solved by removing the class disjoint assertions, but I think I need to report to CERT and let them change and decide.

The ultimate plan is to merge both CERT and data exifiltration ontology together.

# mychoco
**mychoco** directory contains the code to extract and annotate the data. The original data is in CSV. They are orgranized by the data type, rather than the time series data for each user. *dataProcessor.py* code extracts login, device, file, http and email information for a specified user, then merge these information according to their timestamp. An aggregated file is generated and can be used as an activity stream for that user. 

When using *dataProcessor.py*, you need to manually change the *dailyStart* and *dailyEnd* time in line 8 and 9. These two time can be obtained from the login.csv file in the synthesized data. What you can do is to extract the login information for the interested user, then use the first two weeks' starting and ending time to estimate the routine time. This is important for *dataProcessor.py* to determine the after hour actions. When extracting a specific user, you need to type *python dataProcessor.py userid*, where userid denodes the user's id, such as ACM2278. 
The data and script directry tree is as follows:

![alt text](http://i.imgur.com/Nsx5VYR.png "data and script directory")

**background.py** extracts the background information, please refer to the above image for details. 

# Data Annotation
Please refer to our [task log](https://docs.google.com/document/d/1ixi-0bNbfmbNqd623x08u2_HILzQXJL5Cz66eBdt5XU/edit?usp=sharing) for all of the details of data annotation, please refer to 6/24/2016 - 6/16/2016 logs.

# Stream Reasoning
to be continued...