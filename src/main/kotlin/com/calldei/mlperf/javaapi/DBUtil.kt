package com.calldei.mlperf.javaapi

import com.beust.jcommander.Parameter
import com.marklogic.client.DatabaseClient
import com.marklogic.client.DatabaseClientFactory


data class Config(
        @Parameter(names=["--hostname"] , description="Marklogic Hostname")
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


        @Parameter(names=["--help"],help=true)
        var help: Boolean = false
        )


fun getClient(config: Config)  =
        DatabaseClientFactory.newClient( config.host , config.port ,
                DatabaseClientFactory.DigestAuthContext(config.user, config.password ) )

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

