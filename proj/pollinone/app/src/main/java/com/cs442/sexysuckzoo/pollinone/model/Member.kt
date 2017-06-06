package com.cs442.sexysuckzoo.pollinone.model

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson

data class Member(val credential: String,
                  var item: Int?) {
    class Deserializer : ResponseDeserializable<Member> {
        override fun deserialize(content: String) = Gson().fromJson(content, Member::class.java)
    }
}
