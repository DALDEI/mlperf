package com.calldei.mlperf.javaapi

import com.beust.jcommander.Parameter
import com.marklogic.client.DatabaseClient
import com.marklogic.client.DatabaseClientFactory
import com.marklogic.client.DatabaseClientFactory.newClient
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.experimental.buildIterator


class Config(
        @Parameter(names=["--host","--hostname"] , description="Marklogic Hostname")
        var host: String = "localhost",
        @Parameter(names=["--port"] , description = "Port with REST API and XDBC compatible")
        var port: Int = 8000 ,
        @Parameter(names=["--user"],description = "Username")
        var user: String = "admin",
        @Parameter(names=["--password"] ,description = "password")
        var password: String = "admin",
        @Parameter(names=["--docs","--documents"],description="Number of documents (total)")
        var documents :Int =  1000,
        @Parameter(names=["--chunksz"],description = "Chunk size in #docs for chunked tests")
        var chuncksz :Int = 10,
        @Parameter(names=["--innersz"],description = "Per document inner array size")
        var innersz : Int = 10,


        @Parameter(names=["--ssl","--https"],arity=0)
        var ssl : Boolean = false ,


        @Parameter(names=["--no-pojo","--no-run-pojo"])
        var noRunPojo : Boolean = false,
        @Parameter(names=["--no-xdbc","--no-run-xdbc"])
        var noRunXdbc : Boolean = false,

        @Parameter(names=["--reset-connection"],arity=1)
        var resetEvery : Int  = 0 ,

@Parameter(names=["--help"],help=true)
        var help: Boolean = false
        ) {
    var counter = 0
    val resetConnection : Boolean
    get() {
        if( resetEvery != 0 ){
            if( counter == 0 ) counter = resetEvery
            else
                if( --counter == 0 ) return true
        }
        return false
    }
}


class TrustAll : X509TrustManager {
    override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {
    }

    override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return arrayOf<X509Certificate>( )
    }

}
fun getSSLContext() = SSLContext.getInstance("TLSv1.2").also { it.init(null, arrayOf(TrustAll()),null) }
fun newClient(config: Config)  =
        newClient( config.host , config.port ,
                DatabaseClientFactory.DigestAuthContext(config.user, config.password ).let {
                    if (config.ssl) {
                        it.withSSLContext(  getSSLContext() , TrustAll() )
                    } else it

                }
        )

fun evalNop(client: DatabaseClient, count: Int ) : Int {
    var n =0
    (1..count).forEach {

        val repo = client.newServerEval()
        //   repo.addVariable("value", it)
        repo.xquery("""
    declare variable  ${'$'}value external ;
    1
            """)
        repo.eval().forEach { n++ }
    }
    return n
}

