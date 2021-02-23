# jPALNS: Parallel Adaptive Large Neighborhood Search

JPalns is a Java port of the C# version which can be found here: https://github.com/larsbeck/PALNS

More information from the orinal implementation is here:
Documentation: http://larsbeck.github.io/PALNS/documentation/api/Palns.html

For algorithm details see: http://orbit.dtu.dk/fedora/objects/orbit:56703/datastreams/file_4129408/content

This implementation will deviate from the paper whenever we find improvements (such as combined weights for tuples of destroy and repair)...

# License
jPALNS is distributed under MIT license and includes two libraries:

1. EA Async which allows the usage of an "await" function for Completable Futures. For license details see "EA License".

For library details see: https://github.com/electronicarts/ea-async

2. async-util which allows easy use of async locks in Java. For license details see "Apache 2.0".

For library details see: https://github.com/IBM/java-async-util
