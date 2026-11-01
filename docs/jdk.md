# JDK with jenv

The project needs Java 21. [jenv](https://www.jenv.be/) switches the JDK per shell, directory, or globally.

Select Java 21 for the current shell only:

```sh
jenv shell 21
```

This sets `JENV_VERSION` for that terminal session. It does not change `.java-version` or the global version, and it ends when the terminal closes.

Check the result:

```sh
java -version
mvn -version
```

If `jenv shell 21` reports that the version is not installed, register the JDK first:

```sh
jenv add /path/to/jdk-21/Contents/Home
jenv versions
```

For Maven to follow jenv, enable the plugin once:

```sh
jenv enable-plugin maven
jenv enable-plugin export
```

Then restart the shell and run `make run`, `make dev`, or `make test` as usual.

See also [Setup](setup.md).
