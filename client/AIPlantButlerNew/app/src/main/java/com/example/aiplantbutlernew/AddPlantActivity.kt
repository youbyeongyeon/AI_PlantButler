package com.example.aiplantbutlernew

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class AddPlantActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_plant)

        val imageView: ImageView = findViewById(R.id.image_view_add_plant)
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
                val resultIntent = Intent()
                resultIntent.putExtra("plantName", plantName)
                resultIntent.putExtra("plantImageUri", selectedImageUri.toString())
                setResult(Activity.RESULT_OK, resultIntent)
                finish() // 액티비티 종료
            } else {
                Toast.makeText(this, "사진과 이름을 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}