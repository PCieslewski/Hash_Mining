GROUP MEMBERS:
-PAWEL CIEWSLEWSKI (2169-8969)
-WILL LIVESEY (1874-1956)

HOW TO RUN:
-Download the zip
-On the server machine type: sbt “run #” 
Notes: # = number of zeros; quotations are required
-On remote machines type: set “run IP”
Note: IP = the IP address of the server machine


We decided to  make the size of the work unit 1000. 
We determined this by changing that number and seeing how the ratio of CPU to real time was effected. 
Having 1000 words being sent to each work led to the best performance for both the local and remote systems.

The result of running: sbt “run 4” is in the file fourResults.txt. 
This is because it is an infinite program and we weren’t sure how much to display. 
Also note that it shows the a connection to another machine has been established.

Result of: time sbt “run 5”
real: 918m33.374s
user: 3250m30.728
sys: 63m39.352s

Ratio of CPU to REAL TIME: 3.489

We found 8 number of bitcoins with 8 leading zeros:
1. pawel:8<8D is 0000000055b0dba962333dc052b90ebd3e4e34cad83f3983a81efcd8b3baca4a
2. pawelL;U9i is 00000000145a4da6bf75801f8d23df7d5d5cab1767443278af3f0eed46df1dbe
3. pawel8N&b7! is 00000000da2a1bddb3318bd684e5cda0946e99b220093478eea08d66d3410dc9
4. pawelHDIcc! is 00000000707f2431a28890e998184aaa697881509803f427ab4bd22dc1b64243
5. Steelerfan2010v}G27 is 000000001d21b7f4e6df2b1729ece2f0722f7e0d8ca61961e5b558dc8beb1bce
6. Steelerfan2010W+N8X is 00000000d61a97ab07ec07cbaedc8114ded8a5ddb700fe77929c5097742cb4e4
7. Steelerfan2010.O1,a is 00000000ebf2f619fefa3cf42ae1b3bd7ecd68eda7804dcd60ac7e2916f5308b
8. Steelerfan2010xYG7o is 0000000042b2557b5caf4b26e4ddf13c8995c4b2c7002e174e4be499aba7ae9a


We were able to run four machines simultaneously with our code. We left it on overnight to try to find the largest possible bitcoins.
