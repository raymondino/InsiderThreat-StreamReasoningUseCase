We have trimed the data into 4 segments:
* 08/17/2010 - 08/25/2010 <- when ACM2278 commits data exfiltration
* 02/07/2011 - 03/05/2011 <- when CMP2946 commits data exfiltration
* 02/21/2011 - 04/26/2011 <- when CDE1846 commits data exfiltration
* 10/21/2010 - 10/13/2010 <- when MBG3183 commits data exfiltration

The reason to trim the data down is because of the gigantic data size.
The original data size is ~100G, where most of them are innocent events/actions.
The data starts from 2010-01, ends at 2011-05. 
By trimming down the data, we can quickly test out methods ability to detect data exfiltration, while avoid the long waiting. 

I am sure you will ask how about the false positive answers? <-- meaning if you trim down the previous records, how do you prove your system will detect some false data exfiltration events that are actually not ?

Good question, and the answer is as follows. Even if we trim down the data, there are still lots of innocent events/actions there. The trimmed data contains enough information to test both false positive and negative rates, and hopefully, our system will not give any of these errors.

Stay tuned. :-)