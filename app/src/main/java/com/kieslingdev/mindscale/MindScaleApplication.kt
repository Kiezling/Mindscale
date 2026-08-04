package com.kieslingdev.mindscale

import android.app.Application

class MindScaleApplication : Application() {
    val container: AppContainer by lazy { DefaultAppContainer(this) }
}
