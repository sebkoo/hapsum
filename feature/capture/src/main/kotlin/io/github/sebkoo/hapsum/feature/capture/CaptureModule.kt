package io.github.sebkoo.hapsum.feature.capture

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * `ViewModelComponent`-scoped, not `SingletonComponent`: CameraX's `Preview`/`ImageCapture` use
 * cases are single-use per camera session, so a fresh [CameraCapture] is bound per
 * [CaptureViewModel] instance instead of reused across screen visits.
 */
@Module
@InstallIn(ViewModelComponent::class)
internal object CaptureModule {
    @Provides
    fun provideCameraCapture(
        @ApplicationContext context: Context,
    ): CameraCapture = CameraXCapture(context)
}
