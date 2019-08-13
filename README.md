Raven
-----
A free software discord client written in Kotlin.

# DISCLAIMER
This software may be seen as **self-botting** and is suspect to violation of Discord's Terms of Service and therefore could lead to the termination of your Discord account.

Binaries will not be provided. **USE AT YOUR OWN RISK**.

```
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
```

# Building and Running 
## Prerequisites
* Java Development Kit 12 (e.g. OpenJDK 12 or OracleJDK 12)

## Running
This will compile and run the application.
### Windows
Open a command line inside the project directory and run:
```cmd
> gradlew run
```
### Unix and Unix-likes (macOS, GNU/Linux)
Open a terminal inside the project directory and run:
```sh
$ ./gradlew run
```

## Building "fat" JAR
This will give you a jar-file with all dependencies included.
### Windows
Open a command line inside the project directory and run:
```cmd
> gradlew shadowJar
```
### Unix and Unix-likes (macOS, GNU/Linux)
Open a terminal inside the project directory and run:
```sh
$ ./gradlew shadowJar
```

## License
This is software is free software licensed under [GPL 2.0](LICENSE).
