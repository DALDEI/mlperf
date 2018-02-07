package com.calldei.mlperf.javaapi

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.marklogic.xcc.ContentSourceFactory
import java.net.URI

fun runPerf(title: String, chunksz: Int, test: () -> Int) {

    System.err.println("Running ${title}")
    val start = System.nanoTime()
    val n: Int = test()
    val end = System.nanoTime()
    val elapse = (end - start).toDouble() / 1000_000.0
    println("${title}\t${n}\t${chunksz}\t${elapse}\t${n.toDouble() / (elapse / 1000.0)}")
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
        jc.usage()

    } catch (e: Exception) {
        System.err.println(e)
    }

}


fun runTests(config: Config) {
    val npojos = config.documents
    val ninner = config.innersz
    val chunksz = config.chuncksz
    System.err.println("Running test set with ${config.documents} documents, inner array size ${config.innersz} chunk size ${chunksz}")
    // Array of Java Objects
    val pojos = Array<POJO>(npojos) { newPOJO(randLong(), ninner) }
    val om = jacksonObjectMapper()
    // mapOf  "id" to "{ serialized JSON }"
    val pojoMap = pojos.map { it.id to om.writeValueAsString(it) }.toMap()
    val pojoNodes = pojos.map { om.valueToTree<JsonNode>(it) }.toTypedArray()


    println("test\tdocs\tchunks\telapsed ms\tdocs/sec")
    if (true) { // POJO and Document API
        val client = getClient(config)
        try {
            runPerf("writePOJONoop", 1) {
                writePojoNoopEval(client, pojos)
            }

            runPerf("writePOJO", 1) {
                writePOJOs(client, pojos)
            }
            runPerf("writePOJOAsDataBind", 1) {
                writePojoAsDatabind(client, pojos)
            }

            runPerf("writePojoAsDatabindChunked", chunksz) {
                writePojoAsDatabindChunked(client, pojos, chunksz)
            }
            runPerf("writePojoAsDatabindEval", 1) {
                writePojoAsDatabindEval(client, pojos)
            }
            runPerf("writePojoAsEvalString", 1) {
                writePojoAsEvalString(client, pojoMap)
            }
            runPerf("writePojoAsDatabindEvalChunked", chunksz) {
                writePojoAsDatabindEvalChunked(client, pojos, chunksz)
            }

        } finally {
            client.release()
        }
    }

    if (true) {

        // XCC API
        val uri = URI("xcc://${config.user}:${config.password}@${config.host}:${config.port}")
        val cs = ContentSourceFactory.newContentSource(uri)
        val session = cs.newSession()
        try {
            runPerf("xccWriteJSONAsNoop", 1) {
                xccWriteJSONAsNoop(session, pojoNodes)

            }
            runPerf("xccWriteJSON", chunksz) {
                xccWriteJSON(session, pojoNodes)

            }
            runPerf("xccWriteJSONAsString", 1) {
                xccWriteJSONAsString(session, pojoMap)
            }
            runPerf("xccWriteJSONChunked", chunksz) {
                xccWriteJSONChunked(session, pojoNodes, chunksz)
            }
            runPerf("xccWriteJSONChunked2", chunksz) {
                xccWriteJSONChunked2(session, pojoNodes, chunksz)
            }
            runPerf("xccWriteJSONAsStringChunked", chunksz) {
                xccWriteJSONAsStringChunked(session, pojoMap, chunksz)
            }

            runPerf("xccWriteJSONAsEval", 1) {
                xccWriteJSONAsEval(session, pojoNodes)
            }
            runPerf("xccWriteJSONAsEvalChunked",chunksz) {
                xccWriteJSONAsEvalChunked(session, pojoNodes,chunksz)
            }
        } finally {
            session.close()
        }


    }
}