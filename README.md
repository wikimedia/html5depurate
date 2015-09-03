This is an HTTP frontend for the validator.nu HTML 5 parser. It parses some 
input text and returns the reserialized HTML.

## Compile and test

Ubuntu build/test dependencies:
* openjdk-7-jdk
* maven2
* jsvc

Compile with `mvn compile`. Then `mvn dependency:build-classpath` will display
a classpath suitable for testing. Then the daemon can be started with something
like:

```
/usr/bin/jsvc \
	-cp "$classpath":target/classes \
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

## To do

* Debian packaging
  - A SysV init script wrapping jsvc should be fairly simple.
  - Very strong security guarantees are possible by using a security.policy
    file.
  - Most Maven dependencies are packaged already, with the exception of the
    validator.nu parser itself, which needs to be bundled.

* Collect warnings/errors and provide a JSON serialized return format
  exposed at /info.

* Help out MW a bit by extracting the contents of the body tag. This could be
  provided at /body.

* A servlet version, if someone needs that. An early version depended on a
  servlet container, but I abandoned that approach in favour of the robustness
  and management simplicity of a standalone daemon.
