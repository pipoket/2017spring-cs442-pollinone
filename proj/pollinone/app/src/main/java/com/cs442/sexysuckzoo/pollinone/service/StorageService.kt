package com.cs442.sexysuckzoo.pollinone.service

import com.cs442.sexysuckzoo.pollinone.model.Vote

class StorageService {
    var vote: Vote? = null
    // var member: Member? = null
    private object Holder { val INSTANCE = StorageService() }
    companion object {
        val instance: StorageService by lazy { Holder.INSTANCE }
    }
}
