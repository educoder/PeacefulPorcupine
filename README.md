PeacefulPorcupine
=================

The peace bridge between UIC and ENCORE


Configuring & Running
---------------------

You will need scala 2.10.1 and sbt.

On OS X you can install these using:

`brew install scala sbt`

You should configure the application by copying `src/main/resources/reference.conf`
into `src/main/resources/application.conf` and modifying all of the necessary options
for your environment (XMPP account, WakefulWeasel URL, etc.)

Once you've configured everything in `application.conf`, run and compile:

`sbt run`

The application will log all actions to the console.