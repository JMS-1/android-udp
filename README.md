Experimental (and just PoC without any code refinement) application which provides and UDP endpoint and can send data to another UDP endpoint. Although it's only one application it can be client and server at the same time.

So you start the server which presents its endpoint as text and QR code as well. A client scans the QR code on demand and can then send the current time to the server which will display the latest message received. This should work as long as both phones are WiFi connected to the same network.
