# jPALNS: Parallel Adaptive Large Neighborhood Search

JPalns is a Java port of the C# version which can be found here: https://github.com/larsbeck/PALNS

You can run the example knapsack model like this:

```
mvn clean compile && mvn exec:java -Dexec.mainClass="de.nilsbeck.knapsack_example.KnapsackExample"
```

You can generate the JavaDocs using:
```
mvn javadoc:javadoc
```

Or generate both JAR and JavaDoc using:
```
mvn package
```

After running either command, open target/site/apidocs/index.html in a web browser.

You can also run the tests with:
```
mvn test
```

For algorithm details see: http://orbit.dtu.dk/fedora/objects/orbit:56703/datastreams/file_4129408/content

This implementation will deviate from the paper whenever we find improvements (such as combined weights for tuples of destroy and repair)...

# License
jPALNS is distributed under MIT license
