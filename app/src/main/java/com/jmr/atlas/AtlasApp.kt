package com.jmr.atlas

import android.app.Application
import com.jmr.atlas.data.Repository

class AtlasApp : Application() {
    val repo: Repository by lazy { Repository(this) }
    override fun onCreate() {
        super.onCreate()
        repo.files.cleanOpenCopies()   // decrypted copies opened in other apps never outlive a session
    }
}
