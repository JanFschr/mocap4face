package co.facemoji.mocap4face

import android.content.Context
import co.facemoji.async.Future
import co.facemoji.io.ApplicationContext
import co.facemoji.io.ResourceFileSystem
import co.facemoji.logging.logError
import co.facemoji.system.OpenGLContext
import co.facemoji.tracker.*

/**
 * Wraps a camera source and a face tracker into one object.
 */
class CameraTracker(context: Context, glContext: OpenGLContext) {
    private val cameraWrapper: CameraWrapper = CameraWrapper(context, glContext)
    private val faceTracker: Future<FaceTracker?>
    var trackerDelegate: (OpenGLTexture, FaceTrackerResult?) -> Unit = { _, _ -> }
    var frontFacing = true
    init {
        cameraWrapper.start(frontFacing = frontFacing).logError("Error initializing camera")
        faceTracker = FaceTracker.createVideoTracker(ResourceFileSystem(ApplicationContext(context)), TrackerGPUContext(glContext, context))
                .logError("Error initializing face tracker")
        cameraWrapper.addOnFrameListener(this::onCameraImage)
    }

    /**
     * Called whenever a new camera frame becomes available
     */
    private fun onCameraImage(cameraTexture: OpenGLTexture) {
        val result = faceTracker.currentValue?.track(cameraTexture)
        trackerDelegate(cameraTexture, result)
    }

    /**
     * Stops the tracking and releases the camera
     */
    fun stop() {
        cameraWrapper.stop()
    }

    /**
     * (Re)-starts the camera (useful after the app gets suspended)
     */
    fun restart() {
        cameraWrapper.start(frontFacing)
    }

    /**
     * Switches between front and back cameras
     */
    fun switchCamera() {
        frontFacing = !frontFacing
        cameraWrapper.start(frontFacing)
    }

    /**
     * Reference to the OpenGL context used to create the camera surface texture
     */
    val openGLContext = cameraWrapper.openglContext

    /**
     * List of blendshape names used for the list of blendshape sliders
     */
    val blendshapeNames: Future<List<String>> get() = faceTracker.map { it?.blendshapeNames ?: emptyList() }
}