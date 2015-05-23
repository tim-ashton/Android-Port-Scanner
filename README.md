Android-Port-Scanner
====================

An Android application for finding all open ports on a give IP address.

This is a full working prototype of a "port scanner" for Android. This port scanner was made with Android Studio and tested on Samsung Galaxy s3.

The app allows user to input a hostname or an IP address and then scan a range of up to 100 ports. 

When the remote host is entered, the app attempts to ping the host and determine the lowest timeout for scanning. While this task is running users cannot scan so it appears that the IU is not working. You must change focus of the input fields a few times to give the ping time to complete (this is a prototype).
