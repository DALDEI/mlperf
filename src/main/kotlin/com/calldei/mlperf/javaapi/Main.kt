package com.calldei.mlperf.javaapi

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.marklogic.client.DatabaseClient
import com.marklogic.xcc.ContentSourceFactory
import com.marklogic.xcc.SecurityOptions
import com.marklogic.xcc.Session
import java.net.URI
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger
import javax.xml.crypto.Data

val log = Logger.getLogger("com.calldei.mlperf.javaapi")
fun <T> T.reset(): Unit {
  if (this is DatabaseClient)
    this.release()
  else
    if (this is Session) {
      this.close()
    } else
      Unit
}

var results = System.out

class TestClient<T>(val config: Config, val init: () -> T) {
  var resetCount = 0
  var client: T = init()

  fun resetClient(): Unit {
    if (config.resetConnection) {
      client.reset()
      log.log(Level.FINE, "Reseting Client Connection")
      client = init()
    }
  }

  fun runPerf(title: String, chunksz: Int, test: () -> Int) {
    resetClient()
    log.log(Level.INFO, "Running ${title}...")
    val start = System.nanoTime()
    var n: Int = 0

    run {
      n = test()
    }
    val end = System.nanoTime()
    val elapse = (end - start).toDouble() / 1000_000.0
    results.println("${title}\t${n}\t${chunksz}\t${elapse}\t${n.toDouble() / (elapse / 1000.0)}")

  }
}


fun main(args: Array<String>) {
  val config = Config()

  val jc = JCommander.newBuilder().addObject(config).build()
  // add if using java9
  //  jc.usageFormatter = com.beust.jcommander.UnixStyleUsageFormatter(jc)
  try {
    jc.parse(*args)
    if (config.help) throw ParameterException("")
    runTests(config)
  } catch (e: ParameterException) {
    println(e.message)
    jc.usage()

  } catch (e: Exception) {
    log.log(Level.SEVERE, "Exception running test", e)
  }

}

fun newSession(config: Config): Session {
  val xcc = when (config.ssl) {
    true -> "xccs"
    else -> "xcc"
  }
  // XCC API
  val uri = URI("${xcc}://${config.user}:${config.password}@${config.host}:${config.port}")
  val cs = ContentSourceFactory.newContentSource(uri,
      if (config.ssl)
        SecurityOptions(getSSLContext())
      else null)
  val session = cs.newSession()
  return session
}


fun runTests(config: Config) {
  val npojos = config.documents
  val ninner = config.innersz
  val chunksz = config.chuncksz
  val ssl = if (config.ssl) "Using SSL" else "NOT Using SSL"
  val doReset = if (config.resetConnection) "Resetting connection after every test" else ""

  log.info("Running test to host ${config.host}:${config.port} set with ${config.documents} documents, inner array size ${config.innersz} chunk size ${chunksz} ${ssl} ${doReset} ")
  // Array of Java Objects
  val pojos = Array<POJO>(npojos) { newPOJO(randLong(), ninner) }
  val om = jacksonObjectMapper()
  // mapOf  "id" to "{ serialized JSON }"
  val pojoMap = pojos.map { it.id to om.writeValueAsString(it) }.toMap()
  // arrayOf JsonNode's of POJO
  val pojoNodes = pojos.map { om.valueToTree<JsonNode>(it) }.toTypedArray()

  println("test\tdocs\tchunks\telapsed ms\tdocs/sec")
  if (!config.noRunPojo) { // POJO and Document API
    TestClient<DatabaseClient>(config, { newClient(config) }).run {
      try
      {

        log.info("Warming up Java API...")
        warmupPOJO(client, config.documents)


        runPerf("writePOJONoop", 1) {
          writePojoNoopEval(pojos)
        }

        runPerf("writePOJO", 1) {
          writePOJOs(pojos)
        }
        runPerf("writePOJOAsDataBind", 1) {
          writePojoAsDatabind(pojos)
        }

        runPerf("writePojoAsDatabindChunked", chunksz) {
          writePojoAsDatabindChunked(pojos, chunksz)
        }
        runPerf("writePojoAsDatabindEval", 1) {
          writePojoAsDatabindEval(pojos)
        }
        runPerf("writePojoAsEvalString", 1) {
          writePojoAsEvalString(pojoMap)
        }
        runPerf("writePojoAsDatabindEvalChunked", chunksz) {
          writePojoAsDatabindEvalChunked(pojos, chunksz)
        }
      } catch( e: Exception ){
        log.severe("Exception running test: " + e.localizedMessage);
        e.printStackTrace(System.err)
      } finally {
        client.reset()
      }
    }
  }
  if (!config.noRunXdbc) {

    TestClient<Session>(config, { newSession(config) }).run {
      try {

        log.info("Warming up XCC...")
        warmupXCC( config.documents )

        runPerf("xccWriteJSONAsNoop", 1) {
          xccWriteJSONAsNoop(pojoNodes)

        }
        runPerf("xccWriteJSON", chunksz) {
          xccWriteJSON(pojoNodes)
        }
        runPerf("xccWriteJSONAsString", 1) {
          xccWriteJSONAsString(pojoMap)
        }
        runPerf("xccWriteJSONChunked", chunksz) {
          xccWriteJSONChunked(pojoNodes, chunksz)
        }
        runPerf("xccWriteJSONChunked2", chunksz) {
          xccWriteJSONChunked2(pojoNodes, chunksz)
        }
        runPerf("xccWriteJSONAsStringChunked", chunksz) {
          xccWriteJSONAsStringChunked(pojoMap, chunksz)
        }

        runPerf("xccWriteJSONAsEval", 1) {
          xccWriteJSONAsEval(pojoNodes)
        }
        runPerf("xccWriteJSONAsEvalChunked", chunksz) {
          xccWriteJSONAsEvalChunked(pojoNodes, chunksz)
        }
      } finally {
        client.reset()
      }
    }
  }
}
