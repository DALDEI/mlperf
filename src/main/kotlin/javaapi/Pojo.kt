package javaapi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.marklogic.client.DatabaseClient
import com.marklogic.client.DatabaseClientFactory
import com.marklogic.client.document.DocumentWriteSet
import com.marklogic.client.io.JacksonDatabindHandle
import com.marklogic.client.io.marker.JSONWriteHandle
import com.marklogic.client.pojo.PojoRepository
import com.marklogic.client.pojo.annotation .Id
import java.time.Instant
private fun getURI( tag: String , id: Long ) = "/java/${tag}/doc-${id}.json"
private fun getURI( tag: String , pojo: POJO) = getURI(tag,pojo.id )


fun writePOJOs(client: DatabaseClient, pojos : Array<POJO> ) : Int{

    val repo = client.newPojoRepository(POJO::class.java, java.lang.String::class.java)

    var n=0
    pojos.forEach {
        repo.write(it)
        n++
    }
    return n

}

fun writePojoAsDatabind(client: DatabaseClient, pojos : Array<POJO>  ) : Int{

    val repo = client.newJSONDocumentManager()
    var n=0
    pojos.asSequence().forEach {
        repo.writeAs( getURI("databind",it),JacksonDatabindHandle<POJO>(it))
        n++
    }
    return n
}

fun writePojoAsDatabindChunked(client: DatabaseClient, pojos : Array<POJO> , chunksz: Int) : Int{

    val repo = client.newJSONDocumentManager()
    var n=0
      pojos.asSequence().chunked(chunksz ).forEach { list ->
          val set = repo.newWriteSet()
          list.forEach {
              set.add(getURI("dbchunk",it), JacksonDatabindHandle<POJO>(it))
              n++
          }
          repo.write(set)

      }
    return n
}
fun writePojoAsEvalString(client: DatabaseClient, pojos : Map<Long,String> ) : Int{

    var n=0

    pojos.entries.forEach() {(id,str) ->
        val repo = client.newServerEval()
        repo.addVariable("url",getURI("eval",id))
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
fun writePojoNoopEval(client: DatabaseClient, pojos : Array<POJO> ) : Int {
    var n = 0
    pojos.forEach {
        val repo = client.newServerEval()
        val res = repo.xquery("1").eval().forEach { n += it.number.toInt()  }
    }
    return n

}


fun writePojoAsDatabindEval(client: DatabaseClient, pojos : Array<POJO> ) : Int{

    var n=0
    pojos.forEach {
        val repo = client.newServerEval()
        repo.addVariable("url",getURI("evalbind",it))
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

fun writePojoAsDatabindEvalChunked(client: DatabaseClient, pojos : Array<POJO>, chunksz: Int  ) : Int{

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
