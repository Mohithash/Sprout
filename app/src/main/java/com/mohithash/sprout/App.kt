package com.mohithash.sprout

import android.app.Application
import androidx.room.Room
import com.mohithash.sprout.ai.AiClient
import com.mohithash.sprout.ai.PlantAi
import com.mohithash.sprout.data.AppDb
import com.mohithash.sprout.data.JsonStore

class App : Application() {
    lateinit var db: AppDb
    lateinit var store: JsonStore
    val client = AiClient()
    val plants by lazy { PlantAi(client) }
    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, AppDb::class.java, "sprout.db").build()
        store = JsonStore(this)
    }
}
