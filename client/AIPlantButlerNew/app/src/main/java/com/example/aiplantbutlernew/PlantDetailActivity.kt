package com.example.aiplantbutlernew

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private val context: Context,
    private val taskList: MutableList<Task>
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>(), ItemMoveCallback.ItemMoveListener {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_task)
        val description: TextView = itemView.findViewById(R.id.text_view_task_desc)
        val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete_task)
        val alarmTimeText: TextView = itemView.findViewById(R.id.text_view_alarm_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        holder.description.text = task.description
        holder.checkBox.isChecked = task.isDone
        updateAlarmTimeText(holder.alarmTimeText, task.alarmTime)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            task.isDone = isChecked
            if (isChecked) {
                if (task.alarmTime != null) {
                    scheduleAlarm(context, task, position)
                }
            } else {
                cancelAlarm(context, position)
            }
        }

        holder.deleteButton.setOnClickListener {
            cancelAlarm(context, position)
            taskList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, taskList.size)
        }

        holder.itemView.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(context, { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                if (calendar.timeInMillis < System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                task.alarmTime = calendar.timeInMillis
                task.isDone = true
                holder.checkBox.isChecked = true
                updateAlarmTimeText(holder.alarmTimeText, task.alarmTime)
                scheduleAlarm(context, task, position)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        Collections.swap(taskList, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    private fun updateAlarmTimeText(textView: TextView, time: Long?) {
        if (time != null) {
            val sdf = SimpleDateFormat("a hh:mm '알림'", Locale.getDefault())
            textView.text = sdf.format(Date(time))
            textView.visibility = View.VISIBLE
        } else {
            textView.visibility = View.GONE
        }
    }

    private fun scheduleAlarm(context: Context, task: Task, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("TASK_DESCRIPTION", task.description)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        task.alarmTime?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, it, pendingIntent)
            }
        }
    }

    private fun cancelAlarm(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    override fun getItemCount() = taskList.size
}

class PlantDetailActivity : AppCompatActivity() {

    private lateinit var plant: Plant
    private var plantPosition: Int = -1
    private val taskList = mutableListOf<Task>()
    private lateinit var taskAdapter: TaskAdapter

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(this, "알림을 받으려면 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_detail)

        checkPermissions()

        plantPosition = intent.getIntExtra("plantPosition", -1)
        val plantJson = intent.getStringExtra("plantJson")
        if (plantJson != null) {
            plant = Gson().fromJson(plantJson, Plant::class.java)
            taskList.addAll(plant.tasks)
        } else {
            finish()
            return
        }

        val plantImageView: ImageView = findViewById(R.id.image_view_detail_plant)
        val plantNameView: TextView = findViewById(R.id.text_view_detail_plant_name)
        val addTaskButton: Button = findViewById(R.id.button_add_task)
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view_tasks)

        plantImageView.setImageURI(Uri.parse(plant.imageUriString))
        plantNameView.text = plant.name

        taskAdapter = TaskAdapter(this, taskList)
        recyclerView.adapter = taskAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val callback = ItemMoveCallback(taskAdapter)
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        addTaskButton.setOnClickListener {
            val editText = EditText(this)
            AlertDialog.Builder(this)
                .setTitle("새 할 일 추가")
                .setMessage("할 일을 추가하고, 목록을 터치하여 알림을 설정하세요.")
                .setView(editText)
                .setPositiveButton("추가") { _, _ ->
                    val newTaskDesc = editText.text.toString()
                    if (newTaskDesc.isNotBlank()) {
                        taskList.add(Task(newTaskDesc))
                        taskAdapter.notifyItemInserted(taskList.size - 1)
                    }
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("알람 권한 필요")
                    .setMessage("정확한 시간에 알림을 받으려면 '알람 및 리마인더' 권한이 필요합니다. 설정으로 이동하시겠습니까?")
                    .setPositiveButton("이동") { _, _ ->
                        Intent().also { intent ->
                            intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                            startActivity(intent)
                        }
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
        }
    }

    override fun finish() {
        plant.tasks.clear()
        plant.tasks.addAll(taskList)
        val resultIntent = Intent()
        resultIntent.putExtra("plantPosition", plantPosition)
        resultIntent.putExtra("plantJson", Gson().toJson(plant))
        setResult(Activity.RESULT_OK, resultIntent)
        super.finish()
    }
}