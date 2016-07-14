# InsiderTreatUseCase
This use case aims to leverage stream reasoning techniques and the concept of semantic importance to detect one attacking type of the insider threat -- data exfiltration

# Dataset
We use synthesized [dataset](https://www.cert.org/insider-threat/tools/).
We used r6.2 (dataset released by v6.2 generator)
A [diagram](https://www.dropbox.com/s/95615yuownzztvi/insider%20threat%20data%20diagram.png?dl=0) shows the overall dataset
We only provide the link as the size is about 100GB.

#Ontology
There two ontology files in two different folders. 
*ontology-data exfiltration alone* contains the knowledge specifically designed for *data exfiltration*, which is identified as one of the many attacking types of the insider threat. This ontology extends the [original cert ontology](http://resources.sei.cmu.edu/library/asset-view.cfm?assetID=454613) which includes the class hierarchy of insider threat indicators. However, right now (7/14/2016), the ontology is not consistant. 
![alt text](http://i.imgur.com/RI3nffZ.png "CERT ontology inconsistency explanation")
