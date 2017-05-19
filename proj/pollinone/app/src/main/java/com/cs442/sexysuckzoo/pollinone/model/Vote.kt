package com.cs442.sexysuckzoo.pollinone.model

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson

data class Vote(val id: Int,
                val title: String,
                val key: String,
                val rootCredential: String,
                val status: String,
                val itemCount: Int,
                val currentItem: Int) {

    class Deserializer : ResponseDeserializable<Vote> {
        override fun deserialize(content: String) = Gson().fromJson(content, Vote::class.java)
    }
}

