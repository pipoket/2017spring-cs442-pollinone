package com.cs442.sexysuckzoo.pollinone.service

import android.content.res.Resources
import android.provider.Settings.Global.getString
import com.cs442.sexysuckzoo.pollinone.R
import com.cs442.sexysuckzoo.pollinone.model.Vote
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.rx.rx_object
import rx.Observable

class PollService {
    private object Holder { val INSTANCE = PollService() }
    companion object {
        val instance: PollService by lazy {Holder.INSTANCE}
    }
    init {
        FuelManager.instance.basePath = Resources.getSystem().getString(R.string.api_root)
    }

    fun createPoll(title: String, itemCount: Int = 2): Observable<Vote> {
        val param = listOf("title" to title, "itemCount" to itemCount)
        return "/vote/createVote".httpPost(param).rx_object(Vote.Deserializer())
    }
}
