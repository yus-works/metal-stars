package com.example.metalstars

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.metalstars.ui.theme.MetalStarsTheme
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.metalstars.ui.theme.MetalStarsTheme
import com.google.android.filament.Engine
import com.google.android.filament.View
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.getDescription
import io.github.sceneview.ar.node.ARCameraNode
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.collision.CollisionSystem
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import java.util.Arrays
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.app.ActivityCompat


private const val kModelFile = "models/simple_satellite_low_poly_free.glb"
private const val kMaxModelInstances = 10

private const val LOCATION_PERMISSION_REQUEST_CODE = 100


class MainActivity : ComponentActivity() {

    private fun setSessionConfig(session: Session, config: Config) {
        config.depthMode =
            when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                true -> Config.DepthMode.AUTOMATIC
                else -> Config.DepthMode.DISABLED
            }
        config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
        config.lightEstimationMode =
            Config.LightEstimationMode.ENVIRONMENTAL_HDR
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            getLastKnownLocation()
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

            var planeRenderer by remember { mutableStateOf(true) }

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
                sessionConfiguration = { session, config -> currentSession = session; setSessionConfig(session, config) },
                cameraNode = cameraNode,
                planeRenderer = planeRenderer,
                onTrackingFailureChanged = { trackingFailureReason = it },
                onSessionUpdated = { session, updatedFrame ->
                    frame = updatedFrame

                    if (constantDebug) {
                        debug = poseDebugInfo(frame!!.camera.pose)
                    }

                    if (childNodes.isEmpty()) {
                        updatedFrame.getUpdatedPlanes()
                            .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
                            ?.let { it.createAnchorOrNull(it.centerPose) }?.let { anchor ->
                                childNodes += createAnchorNode(
                                    engine = engine,
                                    modelLoader = modelLoader,
                                    materialLoader = materialLoader,
                                    modelInstances = modelInstances,
                                    anchor = anchor
                                )
                            }
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
//                text = trackingFailureReason?.let {
//                    it.getDescription(LocalContext.current)
//                } ?: if (childNodes.isEmpty()) {
//                    "Point your camera at a flat surface"
//                } else {
//                    "Tap on a surface to place the model"
//                }
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