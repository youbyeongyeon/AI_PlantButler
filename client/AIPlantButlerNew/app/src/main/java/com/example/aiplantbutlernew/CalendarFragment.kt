package com.example.aiplantbutlernew

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private val prefsName = "diary_prefs"
    private val prefPhotosKey = "photo_uris_map"
    private var currentMonth: Calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    private var selectedDayKey: Long = startOfDayKey(System.currentTimeMillis())
    private lateinit var tvMonth: TextView
    private lateinit var rvGrid: RecyclerView
    private lateinit var detailsContainer: LinearLayout
    private val photoUriMap: MutableMap<Long, MutableList<Uri>> = mutableMapOf()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val photoList = photoUriMap.getOrPut(selectedDayKey) { mutableListOf() }

            if (result.data?.clipData != null) {
                val clipData = result.data!!.clipData!!
                for (i in 0 until clipData.itemCount) {
                    handleImageSelection(clipData.getItemAt(i).uri, photoList)
                }
            } else if (result.data?.data != null) {
                handleImageSelection(result.data!!.data!!, photoList)
            }

            savePhotoUris(requireContext(), selectedDayKey, photoList)
            rvGrid.adapter?.notifyDataSetChanged()
            showEditorBottomSheet(selectedDayKey)
        }
    }

    private fun handleImageSelection(uri: Uri, photoList: MutableList<Uri>) {
        try {
            requireContext().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            photoList.add(uri)
        } catch (_: SecurityException) {
            Toast.makeText(context, "사진 접근 권한을 얻지 못했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoUriMap.putAll(loadPhotoUriMap(requireContext()))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val rootScroll = ScrollView(ctx).apply { layoutParams = ViewGroup.LayoutParams(-1, -1); setPadding(0, 0, 0, dp(72)); isFillViewport = true }
        val page = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; layoutParams = ViewGroup.LayoutParams(-1, -2) }
        rootScroll.addView(page)
        val header = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(ContextCompat.getColor(ctx, R.color.primaryGreen)); gravity = Gravity.CENTER_VERTICAL; layoutParams = LinearLayout.LayoutParams(-1, dp(56)); setPadding(dp(16), 0, dp(16), 0) }
        val btnPrev = ImageButton(ctx).apply { setImageResource(android.R.drawable.ic_media_previous); background = null; contentDescription = "이전 달"; setColorFilter(Color.WHITE) }
        tvMonth = TextView(ctx).apply { layoutParams = LinearLayout.LayoutParams(0, -2, 1f); gravity = Gravity.CENTER; textSize = 20f; setTextColor(Color.WHITE) }
        val btnNext = ImageButton(ctx).apply { setImageResource(android.R.drawable.ic_media_next); background = null; contentDescription = "다음 달"; setColorFilter(Color.WHITE) }
        header.addView(btnPrev); header.addView(tvMonth); header.addView(btnNext)
        page.addView(header)
        val dowRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(-1, -2); setPadding(0, dp(8), 0, dp(8)) }
        listOf("일", "월", "화", "수", "목", "금", "토").forEach { s-> val t = TextView(ctx).apply { layoutParams = LinearLayout.LayoutParams(0, -2, 1f); gravity = Gravity.CENTER; setTextColor(Color.DKGRAY); text = s}; dowRow.addView(t) }
        page.addView(dowRow)
        rvGrid = RecyclerView(ctx).apply { layoutParams = LinearLayout.LayoutParams(-1, -2); layoutManager = GridLayoutManager(ctx, 7); isNestedScrollingEnabled = false }
        page.addView(rvGrid)
        detailsContainer = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(-1, -2); setPadding(dp(16), dp(16), dp(16), dp(16)) }
        page.addView(detailsContainer)
        fun refreshMonthUI() { tvMonth.text = SimpleDateFormat("yyyy년 M월", Locale.KOREA).format(currentMonth.time); rvGrid.adapter = MonthAdapter(buildDaysOfMonth(currentMonth)) }
        btnPrev.setOnClickListener { currentMonth.add(Calendar.MONTH, -1); refreshMonthUI(); renderAllMemos() }
        btnNext.setOnClickListener { currentMonth.add(Calendar.MONTH, 1); refreshMonthUI(); renderAllMemos() }
        refreshMonthUI()
        renderAllMemos()
        return rootScroll
    }

    private fun onDateTapped(dayKey: Long) {
        selectedDayKey = dayKey
        showEditorBottomSheet(dayKey)
    }

    private fun showEditorBottomSheet(dayKey: Long) {
        val ctx = requireContext()
        val sheet = BottomSheetDialog(ctx)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_calendar_editor, null)
        sheet.setContentView(view)

        val tvDate: TextView = view.findViewById(R.id.text_view_sheet_date)
        val rvThumbnails: RecyclerView = view.findViewById(R.id.recycler_view_thumbnails)
        val btnAddPhoto: Button = view.findViewById(R.id.button_add_photo_sheet)
        val btnManagePhotos: Button = view.findViewById(R.id.button_manage_photos_sheet)
        val etDiary: EditText = view.findViewById(R.id.edit_text_diary_sheet)
        val btnSave: Button = view.findViewById(R.id.button_save_sheet)

        val photoList = photoUriMap.getOrPut(dayKey) { mutableListOf() }
        tvDate.text = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(dayKey)
        etDiary.setText(loadDiaryText(ctx, dayKey))
        btnManagePhotos.visibility = if (photoList.isNotEmpty()) View.VISIBLE else View.GONE

        val thumbnailAdapter = ThumbnailAdapter(photoList) { position ->
            val intent = Intent(context, PhotoViewerActivity::class.java).apply {
                putStringArrayListExtra("PHOTO_URIS", ArrayList(photoList.map { it.toString() }))
                putExtra("DATE", tvDate.text.toString())
                putExtra("POSITION", position)
            }
            startActivity(intent)
        }
        rvThumbnails.adapter = thumbnailAdapter
        rvThumbnails.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)

        btnAddPhoto.setOnClickListener { openImagePicker(); sheet.dismiss() }
        btnManagePhotos.setOnClickListener {
            showPhotoManagementDialog(dayKey, photoList)
            sheet.dismiss()
        }
        btnSave.setOnClickListener {
            saveDiaryText(ctx, dayKey, etDiary.text.toString())
            renderAllMemos()
            Toast.makeText(ctx, "저장되었습니다.", Toast.LENGTH_SHORT).show()
            sheet.dismiss()
        }
        sheet.show()
    }

    private fun showPhotoManagementDialog(dayKey: Long, photoList: MutableList<Uri>) {
        val ctx = requireContext()
        val dialogView = layoutInflater.inflate(R.layout.dialog_photo_management, null)
        val recyclerViewPhotos: RecyclerView = dialogView.findViewById(R.id.recycler_view_photos)
        val titleView: TextView = dialogView.findViewById(R.id.text_view_dialog_title)
        val subtitleView: TextView = dialogView.findViewById(R.id.text_view_dialog_subtitle)

        val photoAdapter = PhotoManagementAdapter(photoList)
        recyclerViewPhotos.adapter = photoAdapter
        recyclerViewPhotos.layoutManager = GridLayoutManager(ctx, 3)

        AlertDialog.Builder(ctx)
            .setView(dialogView)
            .setPositiveButton("순서 저장") { _, _ ->
                val finalOrderedList = photoAdapter.getFinalOrderedList()
                photoUriMap[dayKey] = finalOrderedList
                savePhotoUris(ctx, dayKey, finalOrderedList)
                rvGrid.adapter?.notifyDataSetChanged()
                showEditorBottomSheet(dayKey)
            }
            .setNegativeButton("선택 삭제") { _, _ ->
                val selectedPhotos = photoAdapter.getSelectedPhotos()
                if (selectedPhotos.isEmpty()) {
                    Toast.makeText(ctx, "삭제할 사진을 선택하세요.", Toast.LENGTH_SHORT).show()
                    showPhotoManagementDialog(dayKey, photoList)
                } else {
                    photoList.removeAll(selectedPhotos)
                    savePhotoUris(ctx, dayKey, photoList)
                    rvGrid.adapter?.notifyDataSetChanged()
                    showEditorBottomSheet(dayKey)
                }
            }
            .setNeutralButton("취소", null)
            .show()
    }

    private fun renderAllMemos() {
        val ctx = requireContext()
        detailsContainer.removeAllViews()
        val currentYear = currentMonth.get(Calendar.YEAR)
        val currentMonthValue = currentMonth.get(Calendar.MONTH)
        val items: List<Pair<Long, String>> = prefs(ctx).all.mapNotNull { (k, v) -> if (k.startsWith("text_")) { val dayKey = k.removePrefix("text_").toLongOrNull(); val memo = v as? String; if (dayKey != null && !memo.isNullOrBlank()) { val memoCal = Calendar.getInstance().apply { timeInMillis = dayKey }; if (memoCal.get(Calendar.YEAR) == currentYear && memoCal.get(Calendar.MONTH) == currentMonthValue) { dayKey to memo } else null } else null } else null }.sortedBy { it.first }
        if (items.isEmpty()) { detailsContainer.visibility = View.GONE; return }
        detailsContainer.visibility = View.VISIBLE
        val header = TextView(ctx).apply { text = "이번 달 성장 기록"; textSize = 18f; setTextColor(Color.BLACK) }
        detailsContainer.addView(header)
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        items.forEachIndexed { index, (dayKey, memoText) ->
            if (index > 0) { val divider = View(ctx).apply { setBackgroundColor(0xFFE0E0E0.toInt()); layoutParams = LinearLayout.LayoutParams(-1, dp(1)).apply { topMargin = dp(8); bottomMargin = dp(8) } }; detailsContainer.addView(divider) }
            val tvTitle = TextView(ctx).apply { text = df.format(Date(dayKey)) }; detailsContainer.addView(tvTitle)
            val tvMemo = TextView(ctx).apply { text = memoText }; detailsContainer.addView(tvMemo)
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "image/*"; putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) }
        pickImageLauncher.launch(intent)
    }

    private fun buildDaysOfMonth(monthCal: Calendar): List<Long?> {
        val cal = monthCal.clone() as Calendar; cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); val leadingEmptyDays = firstDayOfWeek - Calendar.SUNDAY
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH); val list = mutableListOf<Long?>()
        repeat(leadingEmptyDays) { list.add(null) }
        for (day in 1..daysInMonth) { val dayCal = cal.clone() as Calendar; dayCal.set(Calendar.DAY_OF_MONTH, day); list.add(dayCal.timeInMillis) }
        while (list.size % 7 != 0) { list.add(null) }
        return list
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    private fun diaryTextKey(dayKey: Long) = "text_$dayKey"
    private fun saveDiaryText(ctx: Context, dayKey: Long, text: String) { prefs(ctx).edit().putString(diaryTextKey(dayKey), text).apply() }
    private fun loadDiaryText(ctx: Context, dayKey: Long): String = prefs(ctx).getString(diaryTextKey(dayKey), "") ?: ""
    private fun savePhotoUris(ctx: Context, dayKey: Long, uris: List<Uri>?) { val allPhotos = loadPhotoUriMap(ctx).toMutableMap(); if (uris.isNullOrEmpty()) { allPhotos.remove(dayKey) } else { allPhotos[dayKey] = uris.toMutableList() }; val jsonString = Gson().toJson(allPhotos.mapValues { entry -> entry.value.map { it.toString() } }); prefs(ctx).edit().putString(prefPhotosKey, jsonString).apply() }
    private fun loadPhotoUriMap(ctx: Context): Map<Long, MutableList<Uri>> { val jsonString = prefs(ctx).getString(prefPhotosKey, null) ?: return mutableMapOf(); return try { val type = object : TypeToken<Map<String, List<String>>>() {}.type; val stringMap: Map<String, List<String>> = Gson().fromJson(jsonString, type); stringMap.mapKeys { it.key.toLong() }.mapValues { entry -> entry.value.map { Uri.parse(it) }.toMutableList() }.toMutableMap() } catch (e: Exception) { mutableMapOf() } }
    private fun startOfDayKey(millis: Long): Long { val c = Calendar.getInstance().apply { timeInMillis = millis; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }; return c.timeInMillis }
    private fun dp(v: Int): Int = (resources.displayMetrics.density * v).toInt()

    private inner class MonthAdapter(private val items: List<Long?>) : RecyclerView.Adapter<MonthAdapter.VH>() {
        inner class VH(val root: FrameLayout, val tv: TextView, val iv: ImageView) : RecyclerView.ViewHolder(root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH { val ctx = parent.context; val cell = FrameLayout(ctx).apply { layoutParams = ViewGroup.LayoutParams(-1, dp(80)); setPadding(dp(1), dp(1), dp(1), dp(1)) }; val iv = ImageView(ctx).apply { layoutParams = FrameLayout.LayoutParams(-1, -1); scaleType = ImageView.ScaleType.CENTER_CROP }; val tv = TextView(ctx).apply { layoutParams = FrameLayout.LayoutParams(-2, -2).apply { gravity = Gravity.TOP or Gravity.START ; setMargins(dp(4), dp(4), 0, 0) }; textSize = 12f; setBackgroundColor(Color.parseColor("#99FFFFFF")); setPadding(dp(4),dp(1),dp(4),dp(1)) }; cell.addView(iv); cell.addView(tv); return VH(cell, tv, iv) }
        override fun getItemCount(): Int = items.size
        override fun onBindViewHolder(holder: VH, position: Int) {
            val millis = items[position]
            if (millis == null) { holder.tv.text = ""; holder.iv.setImageDrawable(null); holder.root.setOnClickListener(null); holder.root.setBackgroundColor(Color.LTGRAY); return }
            holder.root.setBackgroundColor(Color.WHITE)
            val cal = Calendar.getInstance().apply { timeInMillis = millis }
            val day = cal.get(Calendar.DAY_OF_MONTH); val dow = cal.get(Calendar.DAY_OF_WEEK)
            holder.tv.text = day.toString()
            holder.tv.setTextColor(when (dow) { Calendar.SUNDAY -> Color.RED; Calendar.SATURDAY -> Color.BLUE; else -> Color.BLACK })
            val key = startOfDayKey(millis)
            val thumbUri: Uri? = photoUriMap[key]?.firstOrNull()
            if (thumbUri != null) { try { holder.iv.setImageURI(thumbUri) } catch (e: Exception) { holder.iv.setImageDrawable(null) } } else { holder.iv.setImageDrawable(null) }
            holder.root.setOnClickListener { onDateTapped(key) }
        }
    }
}