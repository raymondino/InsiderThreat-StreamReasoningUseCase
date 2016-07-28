This folder contains the benchmar result. 
The file name follows the pattern:

SemanticImportance_windowSize_User_AverageActionProcessTime.txt

Semantic Importance can be 
* prov : provenance
* trust: trust

window size can be
* 1d -> one day
* 7d -> seven days
* 1m -> one month
* 2m -> two month

User can be 
* a single user id, we used insider's action streaming data to test the precision and recall of the system.
* a multiple user number, we incorporated both insider and un-insider action streaming data, to test the scalibility of the system.

Average action process time describes the total processing time (loading, querying) of each action in the system, the unit is ms. 