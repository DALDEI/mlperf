package com.calldei.mlperf.javaapi

import java.util.*

val randGen = Random()

fun randInt()  = Math.abs(randGen.nextInt())
fun randInt(lim: Int ) = Math.abs(randGen.nextInt(lim))
fun randLong() = Math.abs(randGen.nextLong())
fun randDouble() = Math.abs(randGen.nextDouble())

fun downto(n: Int ) { }

class StringList(res: String ) {
    val names  = StringList::class.java.getResourceAsStream(res ).use {
        it.bufferedReader(Charsets.UTF_8).readLines()
    }

    fun rand()  = names[randInt(names.size)]
}

val names = StringList("/names.txt")
val words = StringList("/words.txt")

fun randName() = names.rand()
fun randWord() = words.rand()

fun randWords( lim: Int )  : String =
    Array<String>(randInt(lim + 1))  { randWord() }.joinToString(" ")


fun randString( lim: Int ) : String {
    val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwzyz".toCharArray()
    return Array<Char>(randInt(lim + 1)){ letters[randInt(letters.size)] }.joinToString("")
}

public fun newPOJO(id: Long?, numInners: Int): POJO {
    val pojo = POJO()
    pojo.id = id
    pojo.name = randName()
    pojo.value = randWords(10)
    pojo.attrShort = randString(10)
    pojo.attrMedium = randString(50)
    pojo.attrLong = randWords(5)

    pojo.innerArray = arrayOfNulls<POJO.Inner>(numInners)
    for (i in 0 until numInners) {
        pojo.innerArray[i] = newInner()
    }
    return pojo
}
fun newInner() : POJO.Inner {
    val inner = POJO.Inner()
    inner.name = randName()
    inner.attrDouble = randInt() * 1.0
    inner.attrInt = randInt()
    inner.attrLong = randWords(10)
    inner.attrMedium = randString(20) + " " + randString(30)
    inner.attrShort = randString(10)
    inner.value = randWords(5)
    return inner
}


