package com.example.metalstars

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.metalstars.ui.theme.MetalStarsTheme
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.collision.Quaternion
import io.github.sceneview.collision.Vector3
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView


private const val kModelFile = "models/simple_satellite_low_poly_free.glb"
private const val kMaxModelInstances = 10

private const val LOCATION_PERMISSION_REQUEST_CODE = 100


class MainActivity : ComponentActivity() {

    private lateinit var phoneOrientation: PhoneOrientation

    private fun setSessionConfig(config: Config) {
        config.depthMode = Config.DepthMode.DISABLED
        config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
        config.lightEstimationMode = Config.LightEstimationMode.DISABLED
        config.planeFindingMode = Config.PlaneFindingMode.DISABLED

        // disable environmental understanding whatever that is
        config.focusMode = Config.FocusMode.FIXED
        config.augmentedFaceMode = Config.AugmentedFaceMode.DISABLED
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun getLastKnownLocation(): Array<Double> {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        checkLocationPermission()
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (location != null) {
            return arrayOf(location.latitude, location.longitude, location.altitude)
        }
        return arrayOf()
    }

    @Composable
    private fun SurfaceContainer() {
        // A surface container using the 'background' color from the theme
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            // The destroy calls are automatically made when their disposable effect leaves
            // the composition or its key changes.
            val engine = rememberEngine()
            val modelLoader = rememberModelLoader(engine)
            val materialLoader = rememberMaterialLoader(engine)
            val cameraNode = rememberARCameraNode(engine)
            val childNodes = rememberNodes()
            val view = rememberView(engine)
            val collisionSystem = rememberCollisionSystem(view)


            val modelInstances = remember { mutableListOf<ModelInstance>() }
            var trackingFailureReason by remember {
                mutableStateOf<TrackingFailureReason?>(null)
            }
            var frame by remember { mutableStateOf<Frame?>(null) }
            var currentSession by remember { mutableStateOf<Session?>(null) }
            var constantDebug by remember { mutableStateOf<Boolean>(false) }
            var debug by remember { mutableStateOf<String>("") }
            ARScene(
                modifier = Modifier.fillMaxSize(),
                childNodes = childNodes,
                engine = engine,
                view = view,
                modelLoader = modelLoader,
                collisionSystem = collisionSystem,
                sessionConfiguration = { session, config ->
                    currentSession = session
                    setSessionConfig(config)
                },
                cameraNode = cameraNode,
                onTrackingFailureChanged = { trackingFailureReason = it },
                onSessionUpdated = { session, updatedFrame ->
                    frame = updatedFrame

                    if (constantDebug) {
                        // debug = poseDebugInfo(frame!!.camera.pose)

                        val orientationQuaternion = phoneOrientation.getQuaternion()

                        val x = orientationQuaternion[0]
                        val y = orientationQuaternion[1]
                        val z = orientationQuaternion[2]
                        val w = orientationQuaternion[3]

                        val theta = 2 * Math.acos(w.toDouble())

                        val sinThetaOver2 = Math.sin(theta / 2)
                        val ux = x / sinThetaOver2
                        val uy = y / sinThetaOver2
                        val uz = z / sinThetaOver2

                        debug = "\nth: ${String.format("%.3f", Math.toDegrees(theta)).padStart(1)}\nx : ${String.format("%.3f", ux).padStart(1)}\ny : ${String.format("%.3f", uy).padStart(1)}\nz : ${String.format("%.3f", uz).padStart(1)}"


                        var q2 = frame!!.camera.pose.rotationQuaternion


                        val fx = q2[0]
                        val fy = q2[1]
                        val fz = q2[2]
                        val fw = q2[3]

                        val ttheta = 2 * Math.acos(fw.toDouble())

                        val ssinThetaOver2 = Math.sin(ttheta / 2)
                        val uux = fx / ssinThetaOver2
                        val uuy = fy / ssinThetaOver2
                        val uuz = fz / ssinThetaOver2

                        debug += "\n\nth: ${String.format("%.3f", Math.toDegrees(ttheta)).padStart(1)}\nx : ${String.format("%.3f", uux).padStart(1)}\ny : ${String.format("%.3f", uuy).padStart(1)}\nz : ${String.format("%.3f", uuz).padStart(1)}"

                        // NOTE: I figured it out.
                        // The confusing permutation is actually arcore mapping the cameras local
                        // space to world space, so if the phone is on a table with the camera
                        // facing down, phone relative (0, 0, -1) becomes world relative (0, -1, 0)
                        // because the phone's -Z points toward the worlds -Y, same thing happens
                        // when you hold it up against a wall but since world Z is the same as
                        // phone Z it stays the same.
                        // P.S. the camera has to be exposed for any of this to work
                        val forwardVector = Vector3(0f, 0f, -1f)

                        var qq = Quaternion(
                            q2[0],
                            q2[1],
                            q2[2],
                            q2[3]
                        )
                        val worldAlignedVector = Quaternion.rotateVector(qq, forwardVector)

                        val cameraPose = frame!!.camera.pose

                        debug = "x : ${worldAlignedVector.x}\n" +
                                "y : ${worldAlignedVector.y}\n" +
                                "z : ${worldAlignedVector.z}\n"

                        debug += "\n\nx : ${cameraPose.tx()}\n" +
                                "y : ${cameraPose.ty()}\n" +
                                "z : ${cameraPose.tz()}\n"
                    }
                },
                onGestureListener = rememberOnGestureListener(
                    onSingleTapConfirmed = { motionEvent, node ->
                        val camera = frame!!.camera

                        // camera current position and rotation as a pose
                        val cameraPose = camera.pose

                        debug = poseDebugInfo(cameraPose)

                        val translation = floatArrayOf(-3f, 0f, 0f)

                        // rotate the translation vector to align with the camera's current orientation
                        val rotatedTranslation = cameraPose.rotateVector(translation)

                        // create a new pose for the anchor
                        val anchorPose = Pose(
                            floatArrayOf(
                                cameraPose.tx() + rotatedTranslation[0],
                                cameraPose.ty() + rotatedTranslation[1],
                                cameraPose.tz() + rotatedTranslation[2]
                            ),
                            cameraPose.rotationQuaternion
                        )

                        val anchor = currentSession?.createAnchor(anchorPose) ?: return@rememberOnGestureListener

                        childNodes += createAnchorNode(
                            engine = engine,
                            modelLoader = modelLoader,
                            materialLoader = materialLoader,
                            modelInstances = modelInstances,
                            anchor = anchor
                        )

                        debug = phoneOrientation.getQuaternion().contentToString()

                    })
            )
            Text(
                modifier = Modifier
                    .systemBarsPadding()
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(top = 16.dp, start = 32.dp, end = 32.dp),
                textAlign = TextAlign.Left,
                fontSize = 28.sp,
                color = Color.White,
                text = debug,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace
                )
            )
            Box(
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Button( onClick = { constantDebug = !constantDebug } ) {
                    Text("constant debug info")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkLocationPermission()
        setContent {
            MetalStarsTheme {
                SurfaceContainer()
            }
        }

        phoneOrientation = PhoneOrientation(this)
    }

    override fun onResume() {
        super.onResume()
        phoneOrientation.start()
    }

    override fun onPause() {
        super.onPause()
        phoneOrientation.stop()
    }

    fun poseDebugInfo(pose: Pose): String {
        var poseInfo = ""
        val symbols = arrayOf("x", "y", "z", "w")
        for ((i, e) in pose.translation.withIndex()) {
            val el = if (e >= 0) { " $e" } else { "$e" }

            poseInfo += "${symbols[i]}: $el\n"
        }
        poseInfo += "\n"
        for ((i, e) in pose.rotationQuaternion.withIndex()) {
            val el = if (e >= 0) { " $e" } else { "$e" }

            poseInfo += "${symbols[i]}: $el\n"
        }

        return poseInfo
    }

    fun createAnchorNode(
        engine: Engine,
        modelLoader: ModelLoader,
        materialLoader: MaterialLoader,
        modelInstances: MutableList<ModelInstance>,
        anchor: Anchor
    ): AnchorNode {
        val anchorNode = AnchorNode(engine = engine, anchor = anchor)
        val modelNode = ModelNode(
            modelInstance = modelInstances.apply {
                if (isEmpty()) {
                    this += modelLoader.createInstancedModel(kModelFile, kMaxModelInstances)
                }
            }.removeLast(),
            // Scale to fit in a 0.5 meters cube
            scaleToUnits = 0.5f
        ).apply {
            // Model Node needs to be editable for independent rotation from the anchor rotation
            isEditable = true
        }
        val boundingBoxNode = CubeNode(
            engine,
            size = modelNode.extents,
            center = modelNode.center,
            materialInstance = materialLoader.createColorInstance(Color.White.copy(alpha = 0.5f))
        ).apply {
            isVisible = false
        }
        modelNode.addChildNode(boundingBoxNode)
        anchorNode.addChildNode(modelNode)

        listOf(modelNode, anchorNode).forEach {
            it.onEditingChanged = { editingTransforms ->
                boundingBoxNode.isVisible = editingTransforms.isNotEmpty()
            }
        }
        return anchorNode
    }
}