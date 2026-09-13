package me.slz_br.alira.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.accessibility.AccessibilityManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import me.slz_br.alira.MainActivity
import me.slz_br.alira.R
import me.slz_br.alira.data.repository.AliraRepository

/**
 * Migrado de telainicial/.../login.kt (protótipo do pacote
 * com.example.alirateste) para o pacote real do projeto. Duas mudanças em
 * relação ao original:
 *  1) handleLogin() agora chama repository.autenticar(...) de verdade
 *     (antes era só um Toast com postDelayed fingindo um login);
 *  2) removidos os campos/views que não existem em activity_login.xml
 *     (tvAppName, tvEsqueciSenha) — o layout adaptado usa imgLogo no lugar
 *     do texto do nome do app e não tem link de "esqueci a senha" ainda.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var btnAccessibility: ImageButton
    private lateinit var tvSubtitle: TextView
    private lateinit var glassCard: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var tvFormTitle: TextView
    private lateinit var etLogin: EditText
    private lateinit var etSenha: EditText
    private lateinit var cbMostrarSenha: CheckBox
    private lateinit var btnProsseguir: AppCompatButton
    private lateinit var tvCadastro: TextView

    private var accessibilityModeOn = false

    // repository é criado com getInstance(this) — mesma instância/banco em
    // toda a tela, sem precisar reabrir o Room.databaseBuilder aqui.
    private val repository by lazy { AliraRepository.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        bindViews()
        setupListeners()
        suggestAccessibilityModeIfNeeded()
    }

    private fun bindViews() {
        btnAccessibility = findViewById(R.id.btnAccessibility)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        glassCard = findViewById(R.id.glassCard)
        tvFormTitle = findViewById(R.id.tvFormTitle)
        etLogin = findViewById(R.id.etLogin)
        etSenha = findViewById(R.id.etSenha)
        cbMostrarSenha = findViewById(R.id.cbMostrarSenha)
        btnProsseguir = findViewById(R.id.btnProsseguir)
        tvCadastro = findViewById(R.id.tvCadastro)
    }

    private fun setupListeners() {
        btnAccessibility.setOnClickListener {
            accessibilityModeOn = !accessibilityModeOn
            applyAccessibilityMode(accessibilityModeOn)
        }

        cbMostrarSenha.setOnCheckedChangeListener { _, isChecked ->
            etSenha.inputType = if (isChecked) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etSenha.setSelection(etSenha.text.length)
        }

        btnProsseguir.setOnClickListener { handleLogin() }

        tvCadastro.setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }
    }

    /**
     * lifecycleScope.launch { } é o jeito de chamar uma função "suspend"
     * (repository.autenticar é suspend porque faz uma consulta no Room)
     * a partir de uma Activity comum — é o mesmo papel que
     * rememberCoroutineScope().launch { } cumpre nas telas Compose do
     * :wear, só que aqui a Activity já ganha o escopo pronto (lifecycleScope)
     * porque a dependência lifecycle-runtime-ktx foi adicionada no
     * build.gradle.kts deste módulo.
     */
    private fun handleLogin() {
        val login = etLogin.text.toString().trim()
        val senha = etSenha.text.toString()

        if (login.isEmpty()) {
            etLogin.error = "Informe seu e-mail"
            etLogin.requestFocus()
            return
        }
        if (senha.isEmpty()) {
            etSenha.error = "Informe sua senha"
            etSenha.requestFocus()
            return
        }

        btnProsseguir.isEnabled = false
        lifecycleScope.launch {
            val usuario = repository.autenticar(login, senha)
            btnProsseguir.isEnabled = true

            if (usuario == null) {
                Toast.makeText(
                    this@LoginActivity,
                    "E-mail ou senha incorretos",
                    Toast.LENGTH_SHORT,
                ).show()
                return@launch
            }

            Toast.makeText(this@LoginActivity, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                putExtra(EXTRA_USUARIO_ID, usuario.idUsuario)
            }
            startActivity(intent)
            finish()
        }
    }

    /**
     * Alterna um "modo de acessibilidade" simples: aumenta o tamanho de
     * textos, aumenta o contraste do quadro de vidro e anuncia a mudança
     * para leitores de tela. Igual ao protótipo original, só sem tvAppName
     * (não existe mais no layout adaptado).
     */
    private fun applyAccessibilityMode(enabled: Boolean) {
        btnAccessibility.isSelected = enabled

        val bodySize = if (enabled) 17f else 14f
        val fieldTextSize = if (enabled) 19f else 16f
        val buttonTextSize = if (enabled) 19f else 17f

        tvSubtitle.textSize = bodySize
        tvFormTitle.textSize = if (enabled) 24f else 20f
        etLogin.textSize = fieldTextSize
        etSenha.textSize = fieldTextSize
        cbMostrarSenha.textSize = bodySize
        tvCadastro.textSize = bodySize
        btnProsseguir.textSize = buttonTextSize

        glassCard.setBackgroundResource(
            if (enabled) R.drawable.bg_glass_card_contrast else R.drawable.bg_glass_card
        )

        val message = if (enabled) {
            "Modo de acessibilidade ativado. Textos ampliados e contraste maior."
        } else {
            "Modo de acessibilidade desativado."
        }
        btnAccessibility.contentDescription = message
        btnAccessibility.announceForAccessibility(message)
    }

    private fun suggestAccessibilityModeIfNeeded() {
        val a11yManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val screenReaderOn = a11yManager?.isTouchExplorationEnabled == true
        if (screenReaderOn && !accessibilityModeOn) {
            accessibilityModeOn = true
            applyAccessibilityMode(true)
        }
    }

    companion object {
        const val EXTRA_USUARIO_ID = "extra_usuario_id"
    }
}
