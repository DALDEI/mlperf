# mlperf - MarkLogic API Performance Experiments

mlperf contains code snippets of various API and use cases for MarkLoigc with the intent to explore the performance behaviors for differennt styles of APIs and uses.

Initial commit contains examples uploading small JSON documents using the Java API and XCC with a variety of methods including:

* Single document upload

* "Chunked" (batch) docuement upload
* Implicit and explicit data binding

* POJO and Document interfaces
* Eval ('adHoq') Query with document as external variables (both single andchunked)


There is currently a single form for the sample document "POJO" which is a simple Plain Old Java Object ("POJO") containing a few fields and an array of an inner class containing a few fields.

The size of the inner array can be specified with --innersz to simulate more or less complex objects as well as making the object bigger and smaller.   The intent is to vary the complexity (number of java fields in this case) of the object to examine the effect of data mapping and serialization methods. 

## SSL
SSL/HTTPS/XCCS is supported by the --ssl flag.  Host and certificate validation are disabled.  This allows the test to run against production systems which are using SSL and/or SSL terminating load balencers.  SSL adds a per-connection overhead as well as a small flat overhead (varies linearly by size total size of data transfered).

## Usage


### Build
./gradlew installDist

### Run
build/install/mlperf/bin/mlperf [options]

Output is intermixed with informative messages on stderr and TSV (Tab seperated output) on stdout.


### Example Output
Example: (lines prefixed with ">" are on stderr)
````
>Running test set with 1000 documents, inner array size 10 chunk size 10
test	docs	chunks	elapsed ms	docs/sec
>Running writePOJONoop
writePOJONoop	1000	1	1548.822562	645.6517515529323
>Running writePOJO
writePOJO	1000	1	4499.53476	222.2451994125722
>Running writePOJOAsDataBind
writePOJOAsDataBind	1000	1	4251.622473	235.20432643079596
>Running writePojoAsDatabindChunked
writePojoAsDatabindChunked	1000	10	1598.268717	625.6770149872114
>Running writePojoAsDatabindEval
writePojoAsDatabindEval	1000	1	3022.407145	330.86210825510733
>Running writePojoAsEvalString
writePojoAsEvalString	1000	1	2731.730963	366.0682598486182
>Running writePojoAsDatabindEval
writePojoAsDatabindEval	1000	1	3092.188935	323.3955042918488
>Running writePojoAsDatabindEvalChunked
writePojoAsDatabindEvalChunked	1000	10	1225.358812	816.0874922569212
>Running xccWriteJSONAsNoop
xccWriteJSONAsNoop	1000	1	488.191895	2048.374850631226
>Running xccWriteJSON
xccWriteJSON	1000	10	1315.007626	760.4518637217393
>Running xccWriteJSONAsString
xccWriteJSONAsString	1000	1	1118.34644	894.1772998356395
>Running xccWriteJSONChunked
xccWriteJSONChunked	1000	10	1100.735967	908.483078576463
>Running xccWriteJSONChunked2
xccWriteJSONChunked2	1000	10	1534.742218	651.5752210838054
>Running xccWriteJSONAsStringChunked
xccWriteJSONAsStringChunked	1000	10	1041.018183	960.5980148379408
>Running xccWriteJSONAsEval
xccWriteJSONAsEval	1000	1	1597.242932	626.0788387072981
````

### Options

mlperf --help

````$sh

Usage: <main class> [options]
  Options:
    --chunksz
      Chunk size in #docs for chunked tests
      Default: 10
    --docs, --documents
      Number of documents (total)
      Default: 1000
    --help

    --host, --hostname
      Marklogic Hostname
      Default: localhost
    --ssl, --https

      Default: false
    --innersz
      Per document inner array size
      Default: 10
    --no-pojo, --no-run-pojo

      Default: false
    --no-xdbc, --no-run-xdbc

      Default: false
    --password
      password
      Default: admin
    --port
      Port with REST API and XDBC compatible
      Default: 8000
    --reset-connection

      Default: 0
    --user
      Username
      Default: admin




````
