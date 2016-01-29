# CPSTest

Clicks per second test plugin that allows you to run these tests on a server. Depends on ProtocolLib to run. Uses packets to trick the client into thinking there is an invisible giant zombie that the player is able to attack. Client sends Entity Use packets which the server interprets and uses for the test.

# Compilation

Clone the repository and run `mvn clean install`.