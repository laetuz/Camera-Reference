package com.neotica.camerareference

import android.Manifest
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.neotica.camerareference.databinding.ActivityMainBinding
import com.neotica.camerareference.retrofit.ApiConfig
import com.neotica.camerareference.retrofit.FileUploadResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    //Camera step 5 init currentphotopath
    private lateinit var currentPhotoPath: String
    private var getFile: File? = null

    companion object {
        const val CAMERA_X_RESULT = 200
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val REQUEST_CODE_PERMISSIONS = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cameraXButton.setOnClickListener { startCameraX() }
        binding.cameraButton.setOnClickListener { startTakePhoto() }
        binding.galleryButton.setOnClickListener { startGallery() }
        binding.uploadButton.setOnClickListener { uploadImage() }

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun uploadImage() {
        if (getFile != null) {
            val file = reduceFileImage(getFile as File)

            val description = "Ini adalah deksripsi gambar".toRequestBody("text/plain".toMediaType())
            val requestImageFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val imageMultipart: MultipartBody.Part = MultipartBody.Part.createFormData(
                "photo",
                file.name,
                requestImageFile
            )
            val service = ApiConfig().getApiService().uploadImage(imageMultipart, description)
            service.enqueue(object : Callback<FileUploadResponse> {
                override fun onResponse(
                    call: Call<FileUploadResponse>,
                    response: Response<FileUploadResponse>
                ) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody != null && !responseBody.error) {
                            Toast.makeText(this@MainActivity, responseBody.message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, response.message(), Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<FileUploadResponse>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Gagal instance Retrofit", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this@MainActivity, "Silakan masukkan berkas gambar terlebih dahulu.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startGallery() {
        //Gallery Step 4
        val intent = Intent()
        intent.action = ACTION_GET_CONTENT
        intent.type = "image/*"
        val chooser = Intent.createChooser(intent, "Choose a Picture")
        launcherIntentGallery.launch(chooser)
    }

    private fun startTakePhoto() {
        //Camera Step 2
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        //launcherIntentCamera.launch(intent)
        //Camera Step 4
        intent.resolveActivity(packageManager)

        createTempFile(application).also {
            val photoURI: Uri = FileProvider.getUriForFile(
                this@MainActivity,
                "com.neotica.camerareference",
                it
            )
            currentPhotoPath = it.absolutePath
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            launcherIntentCamera.launch(intent)
        }
    }

    private fun startCameraX() {
        val intent = Intent(this, CameraActivity::class.java)
        launcherIntentCameraX.launch(intent)
    }

    //Camera Step 1
    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == MainActivity.CAMERA_X_RESULT) {
/*            val imageBitmap = it.data?.extras?.get("data") as Bitmap
            //Camera step 1.1: Rotate bitmap
            val result = rotateBitmap(
                imageBitmap
            )*/
            //Camera Step 6
            val myFile = File(currentPhotoPath)
            getFile = myFile
            val resultModule = rotateBitmap(BitmapFactory.decodeFile(myFile.path))
            binding.previewImageView.setImageBitmap(resultModule)
        }
    }

    private val launcherIntentCameraX = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == CAMERA_X_RESULT) {
            val myFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.data?.getSerializableExtra("picture", File::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.data?.getSerializableExtra("picture")
            } as File
            val isBackCamera = it.data?.getBooleanExtra("isBackCamera", true) as Boolean

            getFile = myFile
            val result = rotateBitmapX(
                BitmapFactory.decodeFile(getFile?.path), isBackCamera
            )

            binding.previewImageView.setImageBitmap(result)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                Toast.makeText(
                    this,
                    "Tidak mendapatkan permission.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    //Gallery Step 2
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    //Gallery Step 3
    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedImg: Uri = result.data?.data as Uri
            val myFile = uriToFile(selectedImg, this@MainActivity)
            getFile = myFile
            binding.previewImageView.setImageURI(selectedImg)
        }
    }
}