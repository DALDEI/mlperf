package com.calldei.mlperf.javaapi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.marklogic.client.DatabaseClient
import com.marklogic.client.io.JacksonDatabindHandle
import com.marklogic.xcc.ContentCreateOptions
import com.marklogic.xcc.ContentFactory
import com.marklogic.xcc.Session
import com.marklogic.xcc.types.ValueType

/*
 * XCC
 */
private fun getURI(tag: String) = "/xcc/${tag}/"

private fun getURI(tag: String, id: Long) = "/xcc/${tag}/doc-${id}.json"

private fun getURI(tag: String, pojo: JsonNode) = getURI(tag, pojo["id"].asLong())
/*
 * Write each JsonNode one at a time via XCC
 */
fun TestClient<Session>.xccWriteJSON(pojos: Array<JsonNode>): Int {
  val opts = ContentCreateOptions.newJsonInstance()
  var n = 0
  pojos.forEach { pojo ->
    client.insertContent(
        ContentFactory.newJsonContent(getURI("json", pojo), pojo, opts))
    n++
  }
  return n
}

/*
 * Write batches of JsonNode's using insertContent( JsonNode[] )
 */
fun TestClient<Session>.xccWriteJSONChunked(pojos: Array<JsonNode>, chunksz: Int): Int {
  val opts = ContentCreateOptions.newJsonInstance()
  var n = 0
  pojos.asSequence().chunked(chunksz).forEach { list ->
    val content = list.map { ContentFactory.newJsonContent(getURI("jschunk", it), it, opts) }.toTypedArray()
    client.insertContent(content)
    n += content.size
  }
  return n
}

/*
 * Alternate form of batch writing JsonNode which may partially commit the batch
 */
fun TestClient<Session>.xccWriteJSONChunked2(pojos: Array<JsonNode>, chunksz: Int): Int {
  val opts = ContentCreateOptions.newJsonInstance()
  var n = 0
  pojos.asSequence().chunked(chunksz).forEach { list ->
    val content = list.map { ContentFactory.newJsonContent(getURI("jschunk", it), it, opts) }.toTypedArray()
    val errs = client.insertContentCollectErrors(content)
    errs?.forEach { println(it) }

    n += content.size
  }
  return n
}

/*
 * Write using a JSON Formatted String one at a time
 */
fun TestClient<Session>.xccWriteJSONAsString(pojos: Map<Long, String>): Int {
  val opts = ContentCreateOptions.newJsonInstance()
  var n = 0
  pojos.forEach { (id, pojo) ->
    client.insertContent(
        ContentFactory.newContent(getURI("jstring", id), pojo, opts))
    n++
  }
  return n
}


/*
 * Write using an array of Json formatted strings
 */
fun TestClient<Session>.xccWriteJSONAsStringChunked(pojos: Map<Long, String>, chunksz: Int): Int {
  val opts = ContentCreateOptions.newJsonInstance()
  var n = 0
  pojos.entries.asSequence().chunked(chunksz).forEach { list ->
    val content = list.map { (id, pojo) ->
      ContentFactory.newContent(getURI("jstringchunk", id), pojo, opts)
    }.toTypedArray()
    client.insertContent(content)
    n += content.size
  }
  return n
}

/*
* Miminal round trip eval
 */
fun TestClient<Session>.xccWriteJSONAsNoop(pojos: Array<JsonNode>): Int {

  var n = 0
  pojos.forEach {
    val query = client.newAdhocQuery("1")
    n += client.submitRequest(query).next().item.asString().toInt()

  }
  return n
}
fun TestClient<Session>.warmupXCC(count: Int ): Unit {
  (1..count).forEach {
    client.submitRequest(client.newAdhocQuery(
        "xdmp:document-insert(\"/xcc/\" || xdmp:random() || \".xml\", <random>Some random junk {  xdmp:random() } </random>)"
    ))
    client.submitRequest(client.newAdhocQuery(
        "xdmp:directory-delete(\"/xcc/\")"
    ))
  }
}



/*
 * Write using an eval() ('AdHoc Query')  one node at a time,
 * passing the node as a typed external variable
 */
fun TestClient<Session>.xccWriteJSONAsEval(pojos: Array<JsonNode>): Int {

  var n = 0
  pojos.forEach {
    val query = client.newAdhocQuery(""+
      "declare variable \$url as xs:string external ;"+
      "declare variable \$content external ;"+
      "xdmp:document-insert( \$url , xdmp:to-json( \$content ) )"
    )
    query.setNewStringVariable("url", getURI("eval", it))
    query.setNewVariable("content", ValueType.OBJECT_NODE, it)
    client.submitRequest(query)
    n++

  }
  return n
}

/*
 * Write batches of JSON Objects by sending an array of json-object's
 * serialized using the JacksonDatabindHandle
 */
fun TestClient<Session>.xccWriteJSONAsEvalChunked(pojos: Array<JsonNode>, chunksz: Int): Int {

  var n = 0
  pojos.asSequence().chunked(chunksz).forEach { list ->
    /*  Insert N docuemnts by passing a JsonArray of Nodes
        create the URL on the server using 'id' to form the URL */
    val arrayNode = jacksonObjectMapper().createArrayNode()

    list.forEach {
      arrayNode.add(it)
    }

    val query = client.newAdhocQuery("" +
        "declare variable \$rooturl as xs:string external ;" +
        "declare variable \$content external ; " +
        "for \$c in \$content/* " +
        " return xdmp:document-insert( \$rooturl || map:get(\$c,'id') || '.json', xdmp:to-json( \$c ) )"
    )
    query.setNewStringVariable("rooturl", getURI("evalchunk"))
    query.setNewVariable("content", ValueType.ARRAY_NODE, arrayNode)

    client.submitRequest(query)
    n += arrayNode.size()
  }
  return n
}
