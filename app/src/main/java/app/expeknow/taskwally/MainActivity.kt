package app.expeknow.taskwally

import android.app.Application
import android.app.Dialog
import android.app.WallpaperManager
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import androidx.core.graphics.withTranslation
import androidx.core.graphics.createBitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {
    private var bitmap : Bitmap? = null
    private var setWallpaperButton : Button? = null
    private var inputTextView: TextView? = null
    private var mainView: ConstraintLayout? = null
    private var textSizeSeekBar: SeekBar? = null
    private var infoImageView: ImageView? = null
    private val displayMetrics = DisplayMetrics()
    private var width = 0
    private var height = 0
    var backgroundColor = 0
    var textSize = 16
    var textColor = 0

    private lateinit var spWrite : SharedPreferences
    private lateinit var editor : SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setWallpaperButton = findViewById(R.id.button)
        inputTextView = findViewById(R.id.text)
        mainView = findViewById(R.id.cl_mainView)
        textSizeSeekBar = findViewById(R.id.textSizeBar)
        infoImageView = findViewById(R.id.imageView)

        spWrite = getSharedPreferences("LastSavedTask", Application.MODE_PRIVATE)
        editor = spWrite.edit()

        setWallpaperButton?.setOnClickListener {
            createImage()
        }

        windowManager.defaultDisplay.getMetrics(displayMetrics)
        width = displayMetrics.widthPixels
        height = displayMetrics.heightPixels

        val lastTask = spWrite.getString("lastTask", "Enter your tasks...")
        inputTextView?.text = lastTask

        inputTextView?.setOnLongClickListener {
            colorPicker(true)
            return@setOnLongClickListener true
        }

        inputTextView?.setSelectAllOnFocus(true)

        mainView?.setOnLongClickListener {
            colorPicker(false)
            return@setOnLongClickListener true
        }

        infoImageView?.setOnClickListener {
            showInfoDialog()
        }

        inputTextView?.setOnClickListener {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }

        textSizeSeekBar?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seek: SeekBar,
                progress: Int, fromUser: Boolean
            ) {
                textSizeSeekBar?.progress = progress
                textSize = progress
                inputTextView?.textSize = textSize.toFloat()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

    }

    private fun showInfoDialog(){
        val infoDialog = Dialog(this)
        infoDialog.setContentView(R.layout.popup_help_dialog)
        infoDialog.setTitle("Instructions")
        infoDialog.show()
    }

    private fun colorPicker(isTextColor: Boolean){

        var colorSelectorHeading = "Pick Color"
        if(isTextColor){
            colorSelectorHeading = "Select Text Color"
            Toast.makeText(this, "Select text color", Toast.LENGTH_SHORT).show()
        }else{
            colorSelectorHeading = "Select Background Color"
            Toast.makeText(this, "Select background color", Toast.LENGTH_SHORT).show()
        }

        ColorPickerDialog
            .Builder(this)
            .setTitle(colorSelectorHeading)
            .setColorShape(ColorShape.SQAURE)
            .setDefaultColor(R.color.white)
            .setColorListener { color, _ ->
                if(isTextColor){
                    textColor = color
                    inputTextView?.setTextColor(textColor)
                }else{
                    backgroundColor = color
                    mainView?.setBackgroundColor(backgroundColor)
                    inputTextView?.setBackgroundColor(backgroundColor)
                }
            }
            .show()

    }

    private fun createImage() {
        bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap!!)
        val paint = Paint()
        paint.color = backgroundColor
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        writeTextOnImage(canvas, paint)
    }

    private fun writeTextOnImage(canvas: Canvas, paint: Paint){
        paint.color = Color.WHITE
        val scale = resources.displayMetrics.density
        val text = inputTextView?.text.toString()
        editor.putString("lastTask", text)
        editor.commit()

        val textPaint = TextPaint()
        textPaint.isAntiAlias = true
        textPaint.textSize = textSize * scale
        textPaint.color = if(textColor != 0) textColor else Color.parseColor("#FFFFFF")

        val textWidth = canvas.width - (80 * scale).toInt()

        // create a StaticLayout to wrap the text
        val staticLayout = StaticLayout.Builder.obtain(
            text, 0, text.length, textPaint, textWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()

        // calculate the position of the text to center it vertically and horizontally
        val padding = 0 * scale
        val xPos = (canvas.width - staticLayout.width) / 2.6f - padding
        val yPos = (canvas.height - staticLayout.height) / 2f - padding

        // draw the StaticLayout on the canvas
        canvas.withTranslation(xPos, yPos) {
            staticLayout.draw(this)
        }

        setWallpaper()

    }

    private fun setWallpaper(){
        val wallpaperManager = WallpaperManager.getInstance(applicationContext)
        try {
            // Save bitmap to cache file
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "task_wallpaper.png")
            val fileOutputStream = FileOutputStream(file)
            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.close()

            // Get URI for the file using FileProvider
            val imageUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )

            // Create intent to set wallpaper using system UI
            val intent = Intent(WallpaperManager.ACTION_CROP_AND_SET_WALLPAPER)
            intent.setDataAndType(imageUri, "image/*")
            intent.putExtra("mimeType", "image/*")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            // Start the wallpaper chooser activity
            startActivity(Intent.createChooser(intent, "Set as wallpaper"))

        } catch (e: IOException) {
            e.printStackTrace()
            // Fallback to direct method if the above fails
            try {
                wallpaperManager.setBitmap(bitmap, null, true,
                    WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                Snackbar.make(this, mainView!!.rootView,
                    "Wallpaper set successfully.", Snackbar.LENGTH_SHORT).show()
            } catch (f: IOException) {
                f.printStackTrace()
                Snackbar.make(this, mainView!!.rootView,
                    "Failed to set wallpaper: ${f.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }
}