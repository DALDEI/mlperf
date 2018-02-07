package javaapi

import com.fasterxml.jackson.databind.JsonNode
import com.marklogic.client.DatabaseClient
import com.marklogic.client.io.JacksonDatabindHandle
import com.marklogic.xcc.ContentCreateOptions
import com.marklogic.xcc.ContentFactory
import com.marklogic.xcc.Session
import com.marklogic.xcc.types.ValueType

private fun getURI( tag: String , id: Long ) = "/xcc/${tag}/doc-${id}.json"
private fun getURI( tag: String , pojo: JsonNode ) = getURI(tag,pojo["id"].asLong() )

fun xccWriteJSON(session: Session, pojos : Array<JsonNode> ) : Int {
    val opts = ContentCreateOptions.newJsonInstance()
    var n = 0
    pojos.forEach { pojo ->
        session.insertContent(
                ContentFactory.newJsonContent(getURI("json",pojo), pojo, opts))
      n++
    }
    return n
}

fun xccWriteJSONChunked(session: Session, pojos : Array<JsonNode>, chunksz: Int ) : Int {
    val opts = ContentCreateOptions.newJsonInstance()
    var n = 0
    pojos.asSequence().chunked(chunksz ).forEach { list ->
    val content = list.map {  ContentFactory.newJsonContent(getURI("jschunk",it), it, opts ) }.toTypedArray()
        session.insertContent( content )
        n += content.size
    }
    return n
}
fun xccWriteJSONChunked2(session: Session, pojos : Array<JsonNode>, chunksz: Int ) : Int {
    val opts = ContentCreateOptions.newJsonInstance()
    var n = 0
    pojos.asSequence().chunked(chunksz ).forEach { list ->
        val content = list.map {  ContentFactory.newJsonContent(getURI("jschunk",it), it, opts ) }.toTypedArray()
        val errs = session.insertContentCollectErrors( content )
        errs ?.forEach { println( it )}

        n += content.size
    }
    return n
}
fun xccWriteJSONAsString(session: Session, pojos : Map<Long,String> ) : Int {
    val opts = ContentCreateOptions.newJsonInstance()
    var n = 0
    pojos.forEach { (id,pojo) ->
        session.insertContent(
                ContentFactory.newContent(getURI("jstring",id), pojo, opts))
        n++
    }
    return n
}

fun xccWriteJSONAsStringChunked(session: Session, pojos : Map<Long,String> , chunksz : Int ) : Int {
    val opts = ContentCreateOptions.newJsonInstance()
    var n = 0
    pojos.entries.asSequence().chunked(chunksz ).forEach { list ->
        val content = list.map { (id,pojo) ->
            ContentFactory.newContent(getURI("jstringchunk",id), pojo , opts ) }.toTypedArray()
        session.insertContent( content )
        n += content.size
    }
    return n
}
fun xccWriteJSONAsNoop(session: Session, pojos : Array<JsonNode> ) : Int{

    var n=0
    pojos.forEach {

        val query =     session.newAdhocQuery("1"  )
        n+= session.submitRequest(query).next().item.asString().toInt()

    }
    return n
}
fun xccWriteJSONAsEval(session: Session, pojos : Array<JsonNode> ) : Int{

    var n=0
    pojos.forEach {

    val query =     session.newAdhocQuery("""
    declare variable ${'$'}url as xs:string external ;
    declare variable ${'$'}content external ;
    xdmp:document-insert( ${'$'}url , xdmp:to-json( ${'$'}content ) )
            """)
        query.setNewStringVariable("url",getURI("eval",it))
        query.setNewVariable("content", ValueType.OBJECT_NODE, it)
        session.submitRequest(query)
        n ++

    }
    return n
}