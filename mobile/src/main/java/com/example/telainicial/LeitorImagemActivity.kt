package com.example.telainicial

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LeitorImagemActivity : BaseActivity() {

    override val tituloTela = "Tela do leitor de imagem. Use o botão redondo para tirar foto ou o botão mais para escolher uma imagem."

    private lateinit var drawer: DrawerLayout
    private lateinit var previewView: PreviewView
    private lateinit var banner: View
    private lateinit var btnCapturar: View
    private lateinit var btnGaleria: View
    private lateinit var progress: ProgressBar
    private lateinit var resultPanel: View
    private lateinit var txtResultado: TextView
    private lateinit var txtSemCamera: TextView

    private var imageCapture: ImageCapture? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    // OCR (reconhece texto no próprio aparelho, sem internet)
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Leitura em voz alta
    private var tts: TextToSpeech? = null
    private var ttsPronto = false
    private var textoPendente: String? = null
    private var ultimoTexto: String = ""

    private var processando = false

    // ===================== PERMISSÃO DA CÂMERA =====================
    private val pedirPermissao = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) {
            iniciarCamera()
        } else {
            txtSemCamera.visibility = View.VISIBLE
            Toast.makeText(this, "Sem permissão da câmera", Toast.LENGTH_SHORT).show()
        }
    }

    // ===================== GALERIA =====================
    private val escolherImagem = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) lerImagemDaGaleria(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leitorimagem)

        drawer = findViewById(R.id.main)
        previewView = findViewById(R.id.preview_view)
        banner = findViewById(R.id.banner_aviso)
        btnCapturar = findViewById(R.id.btn_capturar)
        btnGaleria = findViewById(R.id.btn_galeria)
        progress = findViewById(R.id.progress)
        resultPanel = findViewById(R.id.result_panel)
        txtResultado = findViewById(R.id.txt_resultado)
        txtSemCamera = findViewById(R.id.txt_sem_camera)

        // ===================== MENU =====================
        findViewById<View>(R.id.btn_menu).setOnClickListener {
            drawer.openDrawer(GravityCompat.START)
        }
        Navegacao.configurarMenu(this) // Perfil, Acessibilidade e logo -> tela inicial

        iniciarVoz()
        mostrarAviso()

        // ===================== BOTÕES =====================
        btnCapturar.setOnClickListener { tirarFoto() }
        btnGaleria.setOnClickListener { escolherImagem.launch("image/*") }

        findViewById<View>(R.id.btn_ouvir).setOnClickListener { falar(ultimoTexto) }
        findViewById<View>(R.id.btn_fechar).setOnClickListener { fecharResultado() }

        // ===================== CÂMERA =====================
        if (temPermissaoCamera()) {
            iniciarCamera()
        } else {
            pedirPermissao.launch(Manifest.permission.CAMERA)
        }
    }

    // ===================== AVISO AZUL: aparece e some =====================
    private fun mostrarAviso() {
        banner.alpha = 0f
        banner.visibility = View.VISIBLE
        banner.animate().alpha(1f).setDuration(300).start()

        handler.postDelayed({
            banner.animate().alpha(0f).setDuration(400).withEndAction {
                banner.visibility = View.GONE
            }.start()
        }, 4000) // 4 segundos
    }

    // ===================== CÂMERA =====================
    private fun temPermissaoCamera() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun iniciarCamera() {
        txtSemCamera.visibility = View.GONE
        val futuro = ProcessCameraProvider.getInstance(this)
        futuro.addListener({
            val provider = futuro.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (e: Exception) {
                Toast.makeText(this, "Não foi possível abrir a câmera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // ===================== TIRAR FOTO -> OCR =====================
    private fun tirarFoto() {
        if (processando) return
        val captura = imageCapture
        if (captura == null) {
            Toast.makeText(this, "Câmera indisponível. Use o botão + para escolher uma imagem.", Toast.LENGTH_SHORT).show()
            return
        }
        setCarregando(true)

        captura.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val rotacao = image.imageInfo.rotationDegrees
                    val bitmap = image.toBitmap()
                    reconhecer(InputImage.fromBitmap(bitmap, rotacao))
                } catch (e: Exception) {
                    runOnUiThread { falhou() }
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                runOnUiThread { falhou() }
            }
        })
    }

    // ===================== GALERIA -> OCR =====================
    private fun lerImagemDaGaleria(uri: Uri) {
        if (processando) return
        setCarregando(true)
        try {
            reconhecer(InputImage.fromFilePath(this, uri))
        } catch (e: IOException) {
            falhou()
        }
    }

    // ===================== OCR =====================
    private fun reconhecer(imagem: InputImage) {
        recognizer.process(imagem)
            .addOnSuccessListener { resultado ->
                val texto = resultado.text.trim()
                runOnUiThread { mostrarResultado(texto) }
            }
            .addOnFailureListener { runOnUiThread { falhou() } }
    }

    private fun mostrarResultado(texto: String) {
        setCarregando(false)
        resultPanel.visibility = View.VISIBLE

        if (texto.isEmpty()) {
            ultimoTexto = "Nenhum texto foi encontrado na imagem."
            txtResultado.text = ultimoTexto
        } else {
            ultimoTexto = texto
            txtResultado.text = texto
        }
        falar(ultimoTexto)
    }

    private fun falhou() {
        setCarregando(false)
        Toast.makeText(this, "Não foi possível ler a imagem. Tente novamente.", Toast.LENGTH_SHORT).show()
    }

    private fun fecharResultado() {
        tts?.stop()
        resultPanel.visibility = View.GONE
    }

    private fun setCarregando(valor: Boolean) {
        processando = valor
        progress.visibility = if (valor) View.VISIBLE else View.GONE
        btnCapturar.alpha = if (valor) 0.5f else 1f
        btnGaleria.alpha = if (valor) 0.5f else 1f
    }

    // ===================== LEITURA EM VOZ ALTA =====================
    private fun iniciarVoz() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale("pt", "BR"))
                ttsPronto = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
                textoPendente?.let { falar(it) }
                textoPendente = null
            }
        }
    }

    private fun falar(texto: String) {
        if (texto.isBlank()) return
        LeitorDeTela.parar() // evita duas vozes ao mesmo tempo
        if (!ttsPronto) {
            textoPendente = texto
            return
        }
        tts?.stop()
        dividirTexto(texto).forEachIndexed { i, parte ->
            val modo = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(parte, modo, null, "alira_$i")
        }
    }

    /** O TextToSpeech tem limite de tamanho por vez, então textos grandes são enviados em partes. */
    private fun dividirTexto(texto: String): List<String> {
        val limite = (TextToSpeech.getMaxSpeechInputLength() - 100).coerceAtLeast(500)
        val partes = mutableListOf<String>()
        val atual = StringBuilder()
        for (linha in texto.lines()) {
            if (atual.length + linha.length + 1 > limite && atual.isNotEmpty()) {
                partes.add(atual.toString())
                atual.clear()
            }
            atual.append(linha).append('\n')
        }
        if (atual.isNotBlank()) partes.add(atual.toString())
        return partes
    }

    override fun onPause() {
        super.onPause()
        tts?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        cameraExecutor.shutdown()
        recognizer.close()
        tts?.shutdown()
    }
}
