package com.cs442.sexysuckzoo.pollinone.service

import com.cs442.sexysuckzoo.pollinone.model.Vote
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.rx.rx_string
import com.github.kittinunf.fuel.rx.rx_object
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

    fun closePoll(id: Int, rootCredential: String): Observable<String> {
        val param = listOf("rootCredential" to rootCredential)
        return "/Vote/close/$id"
                .httpGet(param)
                .rx_string()
    }
}
