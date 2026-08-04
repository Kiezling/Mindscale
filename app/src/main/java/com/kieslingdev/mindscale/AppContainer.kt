package com.kieslingdev.mindscale

import android.content.Context
import com.kieslingdev.mindscale.data.MindScaleDatabase

interface AppContainer {
    val database: MindScaleDatabase
}

class DefaultAppContainer(context: Context) : AppContainer {
    override val database: MindScaleDatabase by lazy {
        MindScaleDatabase.build(context.applicationContext)
    }
}
