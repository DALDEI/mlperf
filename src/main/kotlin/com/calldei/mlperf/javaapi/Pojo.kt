package com.calldei.mlperf.javaapi

import com.marklogic.client.DatabaseClient
import com.marklogic.client.io.JacksonDatabindHandle

private fun getURI( tag: String , id: Long ) = "/java/${tag}/doc-${id}.json"
private fun getURI( tag: String , pojo: POJO) = getURI(tag, pojo.id)


/*
 * JAVA Client API
 */



/*
 Common simple use: Write each POJO as a separate request
 */

fun TestClient<DatabaseClient>.writePOJOs( pojos : Array<POJO> ) : Int{

    val repo = client.newPojoRepository(POJO::class.java, java.lang.String::class.java)
    var n=0
    pojos.forEach {
        repo.write(it)
        n++
    }
    return n

}

/*
 * Use explict Jackson Data binding
 */

fun TestClient<DatabaseClient>. writePojoAsDatabind(pojos : Array<POJO>  ) : Int{

    val repo = client.newJSONDocumentManager()
    var n=0
    pojos.asSequence().forEach {

        repo.writeAs(getURI("databind", it),JacksonDatabindHandle<POJO>(it))
        n++
    }
    return n
}
/*
 * Use explict Jackson Data binding in batches of chunksz
 */
fun TestClient<DatabaseClient>.writePojoAsDatabindChunked(pojos : Array<POJO>, chunksz: Int) : Int{

    val repo = client.newJSONDocumentManager()
    var n=0
      pojos.asSequence().chunked(chunksz ).forEach { list ->
          val set = repo.newWriteSet()
          list.forEach {
              set.add(getURI("dbchunk", it), JacksonDatabindHandle<POJO>(it))
              n++
          }
          repo.write(set)

      }
    return n
}

/*
 * Write each JSON document using a String for the source
 * Parse JSON to json-node in the server
 * Note: In Main.kt the JSON is serialized using Jackson for Kotlin (jacksonDatamapper),
 * this produces a different form then the Java API
 */

fun TestClient<DatabaseClient>.writePojoAsEvalString( pojos : Map<Long,String> ) : Int{

    var n=0

    pojos.entries.forEach() {(id,str) ->
        val repo = client.newServerEval()
        repo.addVariable("url", getURI("eval", id))
        repo.addVariable("content",(str))
        repo.xquery("""
    declare variable  ${'$'}url as xs:string external ;
    declare variable ${'$'}content external ;
   xdmp:document-insert( ${'$'}url , xdmp:unquote( ${'$'}content ) )
            """)

        repo.eval().forEach {
            println(it.string)


        }
        n++
    }
    return n
}

/*
 * Minimal round trip using eval()
 */
fun TestClient<DatabaseClient>.writePojoNoopEval(pojos : Array<POJO> ) : Int {
    var n = 0
    pojos.forEach {
        val repo = client.newServerEval()
        val res = repo.xquery("1").eval().forEach { n += it.number.toInt()  }
    }
    return n

}

/*
 * Use the Java API Jackson Databind Handle which encodes the JSON as a json:object
 * Convert to JSON Node in the server and insert
 */
fun TestClient<DatabaseClient>.writePojoAsDatabindEval(pojos : Array<POJO> ) : Int{

    var n=0
    pojos.forEach {
        val repo = client.newServerEval()
        repo.addVariable("url", getURI("evalbind", it))
        repo.addVariable("content",(JacksonDatabindHandle(it)))
        repo.xquery("""
    declare variable ${'$'}url as xs:string external ;
    declare variable ${'$'}content external ;
    xdmp:document-insert( ${'$'}url , xdmp:to-json( ${'$'}content ) )
            """)

        repo.eval().forEach {
            println(it.string)
        }
        n++
    }
    return n
}
/*
 * Write batches of JSON Objects by sending an array of json-object's
 * serialized using the JacksonDatabindHandle
 */
fun TestClient<DatabaseClient>.writePojoAsDatabindEvalChunked(pojos : Array<POJO>, chunksz: Int  ) : Int{

    var n=0
    pojos.asSequence().chunked(chunksz ).forEach { list ->
        val repo = client.newServerEval()
        repo.addVariable("rooturl","/javaeval/")
        repo.addVariable("content",(JacksonDatabindHandle(list)))
        /* // Insert N docuemnts using 'id' to form the URL

declare variable $rooturl as xs:string external ;
declare variable \$content external ;
for $c in json:array-values($content) +
   return xdmp:document-insert(
      $rooturl || map:get($c,'id') || '.json', xdmp:to-json( $c ) )


        // Really ugly but better then """ ${'$'}var """
   */
        repo.xquery("" +
    "declare variable \$rooturl as xs:string external ;" +
    "declare variable \$content external ; " +
    "for \$c in json:array-values(\$content)" +
     " return xdmp:document-insert( \$rooturl || map:get(\$c,'id') || '.json', xdmp:to-json( \$c ) )"
        )
        repo.eval()
        n += list.size
    }
    return n
}
