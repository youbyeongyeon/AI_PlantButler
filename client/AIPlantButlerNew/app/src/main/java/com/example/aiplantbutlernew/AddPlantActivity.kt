package com.example.aiplantbutlernew

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class AddPlantActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_plant)

        imageView = findViewById(R.id.image_view_add_plant)
        val buttonSelectImage: Button = findViewById(R.id.button_select_image)
        val editTextPlantName: EditText = findViewById(R.id.edit_text_plant_name)
        val buttonSave: Button = findViewById(R.id.button_save_plant)

        // 갤러리에서 사진을 선택하면 실행될 부분
        val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUri = result.data?.data
                imageView.setImageURI(selectedImageUri)
            }
        }

        // '사진 선택' 버튼 클릭 시 갤러리 열기
        buttonSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            imagePickerLauncher.launch(intent)
        }

        // '저장하기' 버튼 클릭
        buttonSave.setOnClickListener {
            val plantName = editTextPlantName.text.toString()
            if (plantName.isNotBlank() && selectedImageUri != null) {
                // 선택한 이미지를 앱 내부 저장소에 복사하고, 그 파일의 영구적인 Uri를 받아옵니다.
                val permanentImageUri = saveImageToInternalStorage(selectedImageUri!!)

                if (permanentImageUri != null) {
                    val resultIntent = Intent()
                    resultIntent.putExtra("plantName", plantName)
                    resultIntent.putExtra("plantImageUri", permanentImageUri.toString())
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish() // 액티비티 종료
                } else {
                    Toast.makeText(this, "사진 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "사진과 이름을 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 이미지를 내부 저장소에 복사하고 파일 Uri를 반환하는 함수
    private fun saveImageToInternalStorage(uri: Uri): Uri? {
        return try {
            // 갤러리 Uri로부터 이미지 데이터를 읽어올 통로(InputStream)를 엽니다.
            val inputStream = contentResolver.openInputStream(uri)
            // 복사할 파일의 이름과 저장될 위치를 정합니다.
            val fileName = "plant_${System.currentTimeMillis()}.jpg"
            val file = File(getDir("plant_images", Context.MODE_PRIVATE), fileName)
            // 파일을 실제로 저장할 통로(OutputStream)를 엽니다.
            val outputStream = FileOutputStream(file)
            // 이미지 데이터를 읽어서 파일에 씁니다 (복사).
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream?.close()
            // 저장된 파일의 영구적인 Uri를 반환합니다.
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}