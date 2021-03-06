package com.cs442.sexysuckzoo.pollinone.service

import com.cs442.sexysuckzoo.pollinone.model.Member
import com.cs442.sexysuckzoo.pollinone.model.Vote
import com.github.kittinunf.fuel.httpDelete
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.rx.rx_string
import com.github.kittinunf.fuel.rx.rx_object
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import rx.Observable

class PollService {
    private object Holder { val INSTANCE = PollService() }
    companion object {
        val instance: PollService by lazy {Holder.INSTANCE}
    }
    init {
    }

    fun createPoll(title: String, itemCount: Int = 2): Observable<Vote> {
        val param = listOf("title" to title, "itemCount" to itemCount)
        return "/Vote/createVote".httpPost(param).rx_object(Vote.Deserializer())
    }

    fun startPoll(id: Int, rootCredential: String): Observable<Vote> {
        val param = listOf("rootCredential" to rootCredential)
        return "/Vote/start/$id"
                .httpGet(param)
                .rx_object(Vote.Deserializer())
    }

    fun collectPoll(id: Int, rootCredential: String): Observable<Vote> {
        val param = listOf("rootCredential" to rootCredential)
        return "/Vote/collect/$id"
                .httpGet(param)
                .rx_object(Vote.Deserializer())
    }

    fun closePoll(id: Int, rootCredential: String): Observable<JsonArray> {
        val param = listOf("rootCredential" to rootCredential)
        return "/Vote/close/$id"
                .httpGet(param)
                .rx_string()
                .map {
                    JsonParser().parse(it).asJsonArray
                }
    }

    fun count(id: Int, rootCredential: String): Observable<Int> {
        val param = listOf("rootCredential" to rootCredential)
        return "/Vote/countVoter/$id"
                .httpGet(param)
                .rx_string()
                .map {
                    JsonParser().parse(it).asInt
                }
    }

    fun fetchPoll(key: String): Observable<Vote> {
        val param = listOf("key" to key)
        return "/Vote/fetch"
                .httpGet()
                .rx_object(Vote.Deserializer())
    }

    fun fetchPoll(id: Int, credential: String): Observable<Vote> {
        val param = listOf("credential" to credential)
        return "/Vote/fetch/$id"
                .httpGet(param)
                .rx_object(Vote.Deserializer())
    }

    fun joinPoll(id: Int, key: String): Observable<Member> {
        val param = listOf("key" to key)
        return "/Vote/join/$id"
                .httpGet(param)
                .rx_object(Member.Deserializer())
    }

    fun isPollStarted(id: Int): Observable<String> {
        return "/Vote/isStarted/$id"
                .httpGet()
                .rx_string()
    }

    fun vote(voteId: Int, credential: String): Observable<Member> {
        val param = listOf("credential" to credential)
        return "/Members/vote/$voteId"
                .httpGet(param)
                .rx_object(Member.Deserializer())
    }

    fun withdraw(voteId: Int, credential: String): Observable<Member> {
        val param = listOf("credential" to credential)
        return "/Members/vote/$voteId"
                .httpDelete(param)
                .rx_object(Member.Deserializer())
    }
}
