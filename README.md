# Insider Threat Use Case
This use case aims to leverage stream reasoning techniques and the concept of semantic importance to detect one attacking type of the insider threat -- data exfiltration

# Dataset
We use synthesized [dataset](https://www.cert.org/insider-threat/tools/).
We used r6.2 (dataset released by v6.2 generator)
The followed diagram shows the overall dataset.
![alt text](http://i.imgur.com/GjohGD2.png "CERT dataset files")
We only provide the link as the size is about 100GB.

However, if you want only to run our program, you don't have to download the whole datasets. We have already pre-processed the dataset and annotated it with our ontology down below. I have provided the link in InsiderThreatStreamReasoning/data/streamingdata-1000user, InsiderThreatStreamReasoning/data/streamingdata-100user, InsiderThreatStreamReasoning/data/streamingdata-10user, InsiderThreatStreamReasoning/data/streamingdata-1user folders. For more details, please refer to these folders' README.md file. 

# Ontology
There two ontology files in two different folders. 
**ontology-data exfiltration alone** contains the knowledge specifically designed for **data exfiltration**, which is identified as one of the many attacking types of the insider threat. This ontology extends the [original cert ontology](http://resources.sei.cmu.edu/library/asset-view.cfm?assetID=454613) which includes the class hierarchy of insider threat indicators. However, right now (7/14/2016), the ontology is not consistent. 

![alt text](http://i.imgur.com/RI3nffZ.png "CERT ontology inconsistency explanation")

*TradeSecretInformation* is reasoned to be both *Asset* and *Information*, while *Asset* and *Information* class are mutually disjoint. 

This problem can be technically solved by removing the class disjoint assertions.

# mychoco
**mychoco** directory contains the code to extract and annotate the data. The original data is in CSV. They are orgranized by the data type, rather than the time series data for each user. *dataProcessor.py* code extracts login, device, file, http and email information for a specified user, then merge these information according to their timestamp. An aggregated file is generated and can be used as an activity stream for that user. 

When using *dataProcessor.py*, you need to manually change the *dailyStart* and *dailyEnd* time in line 8 and 9. These two time can be obtained from the login.csv file in the synthesized data. What you can do is to extract the login information for the interested user, then use the first two weeks' starting and ending time to estimate the routine time. This is important for *dataProcessor.py* to determine the after hour actions. When extracting a specific user, you need to type *python dataProcessor.py userid*, where userid denodes the user's id, such as ACM2278. 
The data and script directry tree is as follows:

![alt text](http://i.imgur.com/Nsx5VYR.png "data and script directory")

**background.py** extracts the background information, please refer to the above image for details. 

# Data Annotation
Please refer to our [task log](https://docs.google.com/document/d/1ixi-0bNbfmbNqd623x08u2_HILzQXJL5Cz66eBdt5XU/edit?usp=sharing) for all of the details of data annotation, please refer to 6/24/2016 - 6/16/2016 logs.
The following picture shows both the data annotation and ontology diagram. 
![alt text](http://i.imgur.com/aoAqtVU.png "data annotation and ontology")

# Install and Run
see InsiderThreatStreamReasoning/README.md for instructions.
