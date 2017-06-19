# Inquire_Viz
### Inquire toolkit based on BIDMach

#### Install and build

This is the repo for [INQUIRE Tool: Early Insight Discovery for Qualitative Research]
For the new inquire backend, please refer to https://github.com/peparedes/inquire-web-backend
And try it on http://commuterweb.stanford.edu/

This toolkit uses [play] web framework and it requires Java 8. We use [maven] for package management. You can modify [pom.xml] to configure the build process. 

We support both CUDA 7.0, 7.5 and 8.0. You will need to change the project version in pom.xml to 1.1.0-cuda7.0  1.1.0-cuda7.5 or 1.1.1-cuda8.0 base on your preference. 

To compile the project, use:
<pre>
git clone https://github.com/BIDData/Inquire_Viz.git
mvn compile
</pre>

To generate the package and pull all the jars into the lib folder, use:
<pre>
mvn package
</pre>

After you get all the jars, the web server can be started using:
<pre>
./sbt -J-Xmx32g run
</pre>

And then select the program you would like to run.



[play]: https://www.playframework.com/
[maven]: https://maven.apache.org/
[pom.xml]: https://github.com/BIDData/BIDMach_Viz/blob/master/pom.xml
[INQUIRE Tool: Early Insight Discovery for Qualitative Research]: http://dl.acm.org/citation.cfm?id=3023272







