package com.tugaskampus.xray

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private lateinit var xrayView: ImageView
    private lateinit var scanline: ScanlineView
    private lateinit var levelBadge: TextView
    private lateinit var btnGallery: MaterialButton
    private lateinit var btnSwitch: MaterialButton

    private lateinit var sliderIntensity: Slider
    private lateinit var sliderPenetration: Slider
    private lateinit var sliderContrast: Slider
    private lateinit var sliderGlow: Slider
    private lateinit var labelIntensity: TextView
    private lateinit var labelPenetration: TextView
    private lateinit var labelContrast: TextView
    private lateinit var labelGlow: TextView
    private lateinit var tintGroup: ChipGroup
    private lateinit var switchScanline: SwitchMaterial

    /** Semua pemrosesan gambar dilakukan di satu thread latar belakang ini. */
    private val worker: ExecutorService = Executors.newSingleThreadExecutor()
    private val processor = XRayProcessor()

    @Volatile private var settings = XRaySettings()
    @Volatile private var lastOutput: Bitmap? = null

    private var cameraProvider: ProcessCameraProvider? = null
    private var useFrontCamera = false

    /** Foto dari galeri; jika tidak null aplikasi berada di "mode foto". */
    @Volatile private var galleryImage: Bitmap? = null
    private val galleryRenderPending = AtomicBoolean(false)

    private var updatingUi = false

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
        }

    private val storagePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) saveCurrentImage()
            else Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show()
        }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) openGalleryImage(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()

        settings = XRaySettings.load(this)
        setupTintChips()
        setupControls()
        applySettingsToUi()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onPause() {
        super.onPause()
        settings.save(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        worker.shutdown()
    }

    // ---------------------------------------------------------------- UI

    private fun bindViews() {
        xrayView = findViewById(R.id.xrayView)
        scanline = findViewById(R.id.scanline)
        levelBadge = findViewById(R.id.levelBadge)
        btnGallery = findViewById(R.id.btnGallery)
        btnSwitch = findViewById(R.id.btnSwitch)
        sliderIntensity = findViewById(R.id.sliderIntensity)
        sliderPenetration = findViewById(R.id.sliderPenetration)
        sliderContrast = findViewById(R.id.sliderContrast)
        sliderGlow = findViewById(R.id.sliderGlow)
        labelIntensity = findViewById(R.id.labelIntensity)
        labelPenetration = findViewById(R.id.labelPenetration)
        labelContrast = findViewById(R.id.labelContrast)
        labelGlow = findViewById(R.id.labelGlow)
        tintGroup = findViewById(R.id.tintGroup)
        switchScanline = findViewById(R.id.switchScanline)
    }

    private fun setupTintChips() {
        RayTint.values().forEach { tint ->
            val chip = Chip(this).apply {
                id = tint.ordinal + 1000
                text = tint.label
                isCheckable = true
                isCheckedIconVisible = true
            }
            tintGroup.addView(chip)
        }
        tintGroup.setOnCheckedStateChangeListener { _, ids ->
            if (updatingUi || ids.isEmpty()) return@setOnCheckedStateChangeListener
            val tint = RayTint.values()[ids.first() - 1000]
            updateSettings(settings.copy(tint = tint))
        }
    }

    private fun setupControls() {
        sliderIntensity.addOnChangeListener { _, v, fromUser ->
            if (fromUser) updateSettings(settings.copy(intensity = v.toInt()))
        }
        sliderPenetration.addOnChangeListener { _, v, fromUser ->
            if (fromUser) updateSettings(settings.copy(penetration = v.toInt()))
        }
        sliderContrast.addOnChangeListener { _, v, fromUser ->
            if (fromUser) updateSettings(settings.copy(contrast = v.toInt()))
        }
        sliderGlow.addOnChangeListener { _, v, fromUser ->
            if (fromUser) updateSettings(settings.copy(glow = v.toInt()))
        }
        switchScanline.setOnCheckedChangeListener { _, checked ->
            if (!updatingUi) updateSettings(settings.copy(scanline = checked))
        }

        fun preset(id: Int, p: XRaySettings) = findViewById<Chip>(id).setOnClickListener {
            updateSettings(p.copy(tint = settings.tint, scanline = settings.scanline), syncUi = true)
        }
        preset(R.id.presetLow, XRaySettings.PRESET_LOW)
        preset(R.id.presetMid, XRaySettings.PRESET_MID)
        preset(R.id.presetHigh, XRaySettings.PRESET_HIGH)
        preset(R.id.presetMax, XRaySettings.PRESET_MAX)

        findViewById<MaterialButton>(R.id.btnReset).setOnClickListener {
            updateSettings(XRaySettings(), syncUi = true)
        }

        btnSwitch.setOnClickListener {
            useFrontCamera = !useFrontCamera
            bindCamera()
        }
        findViewById<MaterialButton>(R.id.btnCapture).setOnClickListener { onCaptureClicked() }
        btnGallery.setOnClickListener {
            if (galleryImage != null) closeGalleryImage() else pickImage.launch("image/*")
        }
    }

    private fun updateSettings(newSettings: XRaySettings, syncUi: Boolean = false) {
        settings = newSettings
        if (syncUi) applySettingsToUi() else updateLabels()
        if (galleryImage != null) renderGalleryImage()
    }

    private fun applySettingsToUi() {
        updatingUi = true
        val s = settings
        sliderIntensity.value = s.intensity.toFloat().coerceIn(0f, 100f)
        sliderPenetration.value = s.penetration.toFloat().coerceIn(0f, 100f)
        sliderContrast.value = s.contrast.toFloat().coerceIn(50f, 300f)
        sliderGlow.value = s.glow.toFloat().coerceIn(0f, 100f)
        tintGroup.check(s.tint.ordinal + 1000)
        switchScanline.isChecked = s.scanline
        updatingUi = false
        updateLabels()
    }

    private fun updateLabels() {
        val s = settings
        labelIntensity.text = "${getString(R.string.label_intensity)}: ${s.intensity}%"
        labelPenetration.text = "${getString(R.string.label_penetration)}: ${s.penetration}%"
        labelContrast.text = "${getString(R.string.label_contrast)}: ${s.contrast}%"
        labelGlow.text = "${getString(R.string.label_glow)}: ${s.glow}%"
        levelBadge.text = "RAY ${s.levelName()} • ${s.intensity}%"
        scanline.enabledEffect = s.scanline && s.intensity > 0
        scanline.setTint(s.tint)
    }

    // ------------------------------------------------------------ Kamera

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            cameraProvider = future.get()
            if (galleryImage == null) bindCamera()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCamera() {
        val provider = cameraProvider ?: return
        val selector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
        else CameraSelector.DEFAULT_BACK_CAMERA

        if (!provider.hasCamera(selector)) {
            useFrontCamera = !useFrontCamera
            Toast.makeText(this, "Kamera tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                        )
                    )
                    .build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        val mirror = useFrontCamera
        analysis.setAnalyzer(worker) { image -> analyzeFrame(image, mirror) }

        provider.unbindAll()
        try {
            provider.bindToLifecycle(this, selector, analysis)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka kamera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun analyzeFrame(image: ImageProxy, mirror: Boolean) {
        image.use {
            if (galleryImage != null) return
            val frame = it.toBitmap()
            val out = processor.process(frame, it.imageInfo.rotationDegrees, mirror, settings)
            frame.recycle()
            lastOutput = out
            runOnUiThread { if (galleryImage == null) xrayView.setImageBitmap(out) }
        }
    }

    // ------------------------------------------------------- Mode foto

    private fun openGalleryImage(uri: Uri) {
        worker.execute {
            val bmp = try {
                decodeScaled(uri, 1600)
            } catch (e: Exception) {
                null
            }
            runOnUiThread {
                if (bmp == null) {
                    Toast.makeText(this, "Gambar tidak bisa dibuka", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                galleryImage = bmp
                cameraProvider?.unbindAll()
                btnGallery.setText(R.string.back_to_camera)
                btnSwitch.isEnabled = false
                renderGalleryImage()
            }
        }
    }

    private fun closeGalleryImage() {
        galleryImage = null
        btnGallery.setText(R.string.open_gallery)
        btnSwitch.isEnabled = true
        bindCamera()
    }

    /** Render ulang foto galeri; permintaan beruntun digabung agar slider tetap lancar. */
    private fun renderGalleryImage() {
        if (!galleryRenderPending.compareAndSet(false, true)) return
        worker.execute {
            galleryRenderPending.set(false)
            val img = galleryImage ?: return@execute
            val out = processor.process(img, 0, false, settings)
            lastOutput = out
            runOnUiThread { if (galleryImage != null) xrayView.setImageBitmap(out) }
        }
    }

    private fun decodeScaled(uri: Uri, maxSide: Int): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width
                val h = info.size.height
                val largest = maxOf(w, h)
                if (largest > maxSide) {
                    val f = maxSide.toFloat() / largest
                    decoder.setTargetSize((w * f).toInt(), (h * f).toInt())
                }
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: error("decode gagal")
    }

    // ---------------------------------------------------------- Simpan

    private fun onCaptureClicked() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        saveCurrentImage()
    }

    private fun saveCurrentImage() {
        worker.execute {
            // Dijalankan di thread pemrosesan, jadi bitmap tidak sedang ditulis.
            val snapshot = lastOutput?.copy(Bitmap.Config.ARGB_8888, false)
            val ok = snapshot != null && writeToGallery(snapshot)
            snapshot?.recycle()
            runOnUiThread {
                Toast.makeText(
                    this,
                    if (ok) R.string.saved else R.string.save_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun writeToGallery(bitmap: Bitmap): Boolean {
        val name = "XRAY_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/XRayCam")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        return try {
            resolver.openOutputStream(uri)?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it)
            } ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }
}
