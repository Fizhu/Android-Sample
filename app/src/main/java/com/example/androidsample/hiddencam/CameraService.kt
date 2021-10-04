package com.example.androidsample.hiddencam

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*


/**
 * Created by fizhu on 04 October 2021
 * https://github.com/Fizhu
 */
class CameraService : Service(), SurfaceHolder.Callback {

    // Camera variables
    // a surface holder
    // a variable to control the camera
    private var mCamera: Camera? = null

    // the camera parameters
    private var parameters: Camera.Parameters? = null
    private var bmp: Bitmap? = null
    var fo: FileOutputStream? = null
    private var FLASH_MODE: String? = null
    private var QUALITY_MODE = 0
    private var isFrontCamRequest = false
    private var pictureSize: Camera.Size? = null
    var sv: SurfaceView? = null
    private var sHolder: SurfaceHolder? = null
    private var windowManager: WindowManager? = null
    var params: WindowManager.LayoutParams? = null
    var cameraIntent: Intent? = null
    var pref: SharedPreferences? = null
    var editor: SharedPreferences.Editor? = null
    var width = 0
    var height: Int = 0

    private fun openFrontFacingCameraGingerbread(): Camera? {
        if (mCamera != null) {
            mCamera?.stopPreview()
            mCamera?.release()
        }
        var cameraCount = 0
        var cam: Camera? = null
        val cameraInfo = Camera.CameraInfo()
        cameraCount = Camera.getNumberOfCameras()
        for (camIdx in 0 until cameraCount) {
            Camera.getCameraInfo(camIdx, cameraInfo)
            if (cameraInfo.facing === Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx)
                } catch (e: RuntimeException) {
                    Log.e(
                        "Camera",
                        "Camera failed to open: " + e.localizedMessage
                    )
                    /*
                     * Toast.makeText(getApplicationContext(),
                     * "Front Camera failed to open", Toast.LENGTH_LONG)
                     * .show();
                     */
                }
            }
        }
        return cam
    }

    private fun setBesttPictureResolution() {
        // get biggest picture size
        width = pref!!.getInt("Picture_Width", 0)
        height = pref!!.getInt("Picture_height", 0)
        if (width == 0 || height == 0) {
            pictureSize = getBiggesttPictureSize(parameters)
            if (pictureSize != null) parameters?.setPictureSize(
                pictureSize?.width ?: 0,
                pictureSize?.height ?: 0
            )
            // save width and height in sharedprefrences
            width = pictureSize?.width ?: 0
            height = pictureSize?.height ?: 0
            editor?.putInt("Picture_Width", width)
            editor?.putInt("Picture_height", height)
            editor?.commit()
        } else {
            // if (pictureSize != null)
            parameters?.setPictureSize(width, height)
        }
    }

    private fun getBiggesttPictureSize(parameters: Camera.Parameters?): Camera.Size? {
        var result: Camera.Size? = null
        for (size in parameters!!.supportedPictureSizes) {
            if (result == null) {
                result = size
            } else {
                val resultArea: Int = result.width * result.height
                val newArea: Int = size.width * size.height
                if (newArea > resultArea) {
                    result = size
                }
            }
        }
        return result
    }

    /** Check if this device has a camera  */
    private fun checkCameraHardware(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(
            PackageManager.FEATURE_CAMERA_ANY
        )
    }

    /** Check if this device has front camera  */
    private fun checkFrontCamera(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(
            PackageManager.FEATURE_CAMERA_FRONT
        )
    }

    var handler: Handler = Handler()

    @Synchronized
    private fun takeImage(intent: Intent) {
        if (checkCameraHardware(applicationContext)) {
            val extras = intent.extras
            if (extras != null) {
                val flash_mode = extras.getString("FLASH")
                FLASH_MODE = flash_mode
                val front_cam_req = extras.getBoolean("Front_Request")
                isFrontCamRequest = front_cam_req
                val quality_mode = extras.getInt("Quality_Mode")
                QUALITY_MODE = quality_mode
            }
            if (isFrontCamRequest) {

                // set flash 0ff
                FLASH_MODE = "off"
                // only for gingerbread and newer versions
                mCamera = openFrontFacingCameraGingerbread()
                if (mCamera != null) {
                    try {
                        mCamera?.setPreviewDisplay(sv!!.holder)
                    } catch (e: IOException) {
                        handler.post(Runnable {
                            Toast.makeText(
                                applicationContext,
                                "API dosen't support front camera",
                                Toast.LENGTH_LONG
                            ).show()
                        })
                        stopSelf()
                    }
                    val parameters: Camera.Parameters = mCamera!!.parameters
                    pictureSize = getBiggesttPictureSize(parameters)
                    if (pictureSize != null) parameters
                        .setPictureSize(pictureSize?.width ?: 0, pictureSize?.height ?: 0)

                    // set camera parameters
                    mCamera?.parameters = parameters
                    mCamera?.startPreview()
                    mCamera?.takePicture(null, null, mCall)

                    // return 4;
                } else {
                    mCamera = null
                    handler.post {
                        Toast.makeText(
                            applicationContext,
                            "Your Device dosen't have Front Camera !",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    stopSelf()
                }
                /*
             * sHolder = sv.getHolder(); // tells Android that this
             * surface will have its data // constantly // replaced if
             * (Build.VERSION.SDK_INT < 11)
             *
             * sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
             */
            } else {
                mCamera = if (mCamera != null) {
                    mCamera?.stopPreview()
                    mCamera?.release()
                    Camera.open()
                } else getCameraInstance()
                try {
                    if (mCamera != null) {
                        mCamera?.setPreviewDisplay(sv!!.holder)
                        parameters = mCamera?.parameters
                        if (FLASH_MODE == null || FLASH_MODE!!.isEmpty()) {
                            FLASH_MODE = "auto"
                        }
                        parameters?.flashMode = FLASH_MODE
                        // set biggest picture
                        setBesttPictureResolution()
                        // log quality and image format
                        Log.d("Qaulity", parameters?.jpegQuality.toString() + "")
                        Log.d("Format", parameters?.pictureFormat.toString() + "")

                        // set camera parameters
                        mCamera?.parameters = parameters
                        mCamera?.startPreview()
                        Log.d("ImageTakin", "OnTake()")
                        mCamera?.takePicture(null, null, mCall)
                    } else {
                        handler.post {
                            Toast.makeText(
                                applicationContext,
                                "Camera is unavailable !",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    // return 4;
                } catch (e: IOException) {
                    // TODO Auto-generated catch block
                    Log.e("TAG", "CmaraHeadService()::takePicture", e)
                }
                // Get a surface
                /*
                 * sHolder = sv.getHolder(); // tells Android that this surface
                 * will have its data constantly // replaced if
                 * (Build.VERSION.SDK_INT < 11)
                 *
                 * sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
                 */
            }
        } else {
            // display in long period of time
            /*
             * Toast.makeText(getApplicationContext(),
             * "Your Device dosen't have a Camera !", Toast.LENGTH_LONG)
             * .show();
             */
            handler.post {
                Toast.makeText(
                    applicationContext,
                    "Your Device dosen't have a Camera !",
                    Toast.LENGTH_LONG
                ).show()
            }
            stopSelf()
        }

        // return super.onStartCommand(intent, flags, startId);
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // sv = new SurfaceView(getApplicationContext());
        cameraIntent = intent
        Log.d("ImageTakin", "StartCommand()")
        pref = applicationContext.getSharedPreferences("MyPref", 0)
        editor = pref?.edit()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params!!.gravity = Gravity.TOP or Gravity.LEFT
        params!!.width = 1
        params!!.height = 1
        params!!.x = 0
        params!!.y = 0
        sv = SurfaceView(applicationContext)
        windowManager!!.addView(sv, params)
        sHolder = sv!!.holder
        sHolder?.addCallback(this)

        // tells Android that this surface will have its data constantly
        // replaced
        return START_STICKY
    }

    var mCall: Camera.PictureCallback =
        Camera.PictureCallback { data, camera -> // decode the data obtained by the camera into a Bitmap
            Log.d("ImageTakin", "Done")
            if (bmp != null) bmp!!.recycle()
            System.gc()
            bmp = decodeBitmap(data)
            val bytes = ByteArrayOutputStream()
            if (bmp != null && QUALITY_MODE == 0) bmp!!.compress(
                Bitmap.CompressFormat.JPEG,
                70,
                bytes
            ) else if (bmp != null && QUALITY_MODE != 0) bmp!!.compress(
                Bitmap.CompressFormat.JPEG,
                QUALITY_MODE,
                bytes
            )
            val imagesFolder = File(
                Environment.getExternalStorageDirectory(), "MYGALLERY"
            )
            if (!imagesFolder.exists()) imagesFolder.mkdirs() // <----
            val image = File(
                imagesFolder, System.currentTimeMillis()
                    .toString() + ".jpg"
            )

            // write the bytes in file
            try {
                fo = FileOutputStream(image)
            } catch (e: FileNotFoundException) {
                Log.e("TAG", "FileNotFoundException", e)
                // TODO Auto-generated catch block
            }
            try {
                fo?.write(bytes.toByteArray())
            } catch (e: IOException) {
                Log.e("TAG", "fo.write::PictureTaken", e)
                // TODO Auto-generated catch block
            }

            // remember close de FileOutput
            try {
                fo?.close()
                if (Build.VERSION.SDK_INT < 19) sendBroadcast(
                    Intent(
                        Intent.ACTION_MEDIA_MOUNTED,
                        Uri.parse(
                            "file://"
                                    + Environment.getExternalStorageDirectory()
                        )
                    )
                ) else {
                    MediaScannerConnection
                        .scanFile(
                            applicationContext, arrayOf(image.toString()),
                            null
                        ) { path, uri ->
                            Log.i(
                                "ExternalStorage", "Scanned "
                                        + path + ":"
                            )
                            Log.i(
                                "ExternalStorage", "-> uri="
                                        + uri
                            )
                        }
                }
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
            if (mCamera != null) {
                mCamera?.stopPreview()
                mCamera?.release()
                mCamera = null
            }
            /*
                 * Toast.makeText(getApplicationContext(),
                 * "Your Picture has been taken !", Toast.LENGTH_LONG).show();
                 */
            Log.d("Camera", "Image Taken !")
            if (bmp != null) {
                bmp!!.recycle()
                bmp = null
                System.gc()
            }
            mCamera = null
            handler.post(Runnable {
                Toast.makeText(
                    applicationContext,
                    "Your Picture has been taken !", Toast.LENGTH_SHORT
                )
                    .show()
            })
            stopSelf()
        }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getCameraInstance(): Camera? {
        var c: Camera? = null
        try {
            c = Camera.open() // attempt to get a Camera instance
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
        }
        return c // returns null if camera is unavailable
    }

    override fun onDestroy() {
        if (mCamera != null) {
            mCamera?.stopPreview()
            mCamera?.release()
            mCamera = null
        }
        if (sv != null) windowManager!!.removeView(sv)
        val intent = Intent("custom-event-name")
        // You can also include some extra data.
        intent.putExtra("message", "This is my message!")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        super.onDestroy()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
//        if (cameraIntent != null) TakeImage().execute(cameraIntent)
        if (cameraIntent != null) {
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Default) {
                    takeImage(cameraIntent!!)
                }
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (mCamera != null) {
            mCamera?.stopPreview()
            mCamera?.release()
            mCamera = null
        }
    }

    private fun decodeBitmap(data: ByteArray?): Bitmap? {
        var bitmap: Bitmap? = null
        val bfOptions = BitmapFactory.Options()
        bfOptions.inDither = false // Disable Dithering mode
        bfOptions.inPurgeable = true // Tell to gc that whether it needs free
        // memory, the Bitmap can be cleared
        bfOptions.inInputShareable = true // Which kind of reference will be
        // used to recover the Bitmap data
        // after being clear, when it will
        // be used in the future
        bfOptions.inTempStorage = ByteArray(32 * 1024)
        if (data != null) bitmap = BitmapFactory.decodeByteArray(
            data, 0, data.size,
            bfOptions
        )
        return bitmap
    }
}