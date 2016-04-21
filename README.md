This is an HTTP frontend for the validator.nu HTML 5 parser. It parses some 
input text and returns the reserialized HTML.

## Compile and test

Ubuntu build/test dependencies:
* openjdk-7-jdk
* maven2
* jsvc

Compile and generate a .jar file by running `mvn package`.

We use the "shade" plugin to bundle all dependencies. So to run it, you only
need the generated .jar file in the class path. To start the server as a
daemon, use something like:

```
/usr/bin/jsvc \
	-cp $(pwd)/target/html5depurate-1.0-SNAPSHOT.jar \
	-pidfile /tmp/html5depurate.pid \
	-errfile /tmp/html5depurate.err \
	-outfile /tmp/html5depurate.out \
	-procname html5depurate \
	org.wikimedia.html5depurate.DepurateDaemon
```

The default log format is pretty bad but can be configured by the usual means,
with -Djava.util.logging.config.file=/path/to/logging.properties

Then to test:

```
curl http://localhost:4339/document -F text=foo
```

This will return an HTML document which is a reserialized version of "foo".

## Configuration

Configuration options may be specified in /etc/html5depurate/html5depurate.conf.
Possible configuration options and their default values are documented below:

```
# Max POST size, in bytes.
maxPostSize = 100000000

# Host or IP and port on which Html5depurate will listen.
host = localhost
port = 4339
```

## To do

* Debian packaging
  - A SysV init script wrapping jsvc should be fairly simple.
  - Very strong security guarantees are possible by using a security.policy
    file.
  - There is no package for grizzly, so we will have to bundle it for now.
    Using Maven Central during build, instead of creating about 9 new Debian
    source packages, is not allowed in Debian upstream, but WMF can distribute
    the resulting file.

* Collect warnings/errors and provide a JSON serialized return format
  exposed at /info.

* Help out MW a bit by extracting the contents of the body tag. This could be
  provided at /body.

* A servlet version, if someone needs that. An early version depended on a
  servlet container, but I abandoned that approach in favour of the robustness
  and management simplicity of a standalone daemon.
