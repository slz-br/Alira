package me.slz_br.alira.presentation

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import me.slz_br.alira.R
import me.slz_br.alira.data.repository.AliraRepository
import me.slz_br.alira.util.Validacoes
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Tela nova de cadastro (RF08) — não existia no protótipo telainicial.
 * Um campo de texto para cada coluna de UsuarioEntity; ao salvar, chama
 * repository.cadastrar(...), que grava no banco do celular e tenta
 * sincronizar com o relógio (ver AliraRepository.kt e
 * data/sync/MobileDataLayerService.kt).
 */
class CadastroActivity : AppCompatActivity() {

    private lateinit var etNome: EditText
    private lateinit var etDataNascimento: EditText
    private lateinit var etTelefone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etDeficiencia: EditText
    private lateinit var etNivelAutonomia: EditText
    private lateinit var etSenha: EditText
    private lateinit var etConfirmarSenha: EditText
    private lateinit var btnCadastrar: AppCompatButton
    private lateinit var tvVoltarLogin: TextView

    private val repository by lazy { AliraRepository.getInstance(this) }

    // dd/mm/aaaa -> timestamp (Long), formato do campo dataNascimento em
    // UsuarioEntity (mesmo tipo usado no :wear).
    private val formatoData = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro)

        bindViews()
        setupListeners()
    }

    private fun bindViews() {
        etNome = findViewById(R.id.etNome)
        etDataNascimento = findViewById(R.id.etDataNascimento)
        etTelefone = findViewById(R.id.etTelefone)
        etEmail = findViewById(R.id.etEmail)
        etDeficiencia = findViewById(R.id.etDeficiencia)
        etNivelAutonomia = findViewById(R.id.etNivelAutonomia)
        etSenha = findViewById(R.id.etSenha)
        etConfirmarSenha = findViewById(R.id.etConfirmarSenha)
        btnCadastrar = findViewById(R.id.btnCadastrar)
        tvVoltarLogin = findViewById(R.id.tvVoltarLogin)
    }

    private fun setupListeners() {
        btnCadastrar.setOnClickListener { handleCadastro() }
        tvVoltarLogin.setOnClickListener { finish() }
    }

    private fun handleCadastro() {
        val nome = etNome.text.toString().trim()
        val dataTexto = etDataNascimento.text.toString().trim()
        val telefone = etTelefone.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val deficiencia = etDeficiencia.text.toString().trim()
        val nivelAutonomia = etNivelAutonomia.text.toString().trim()
        val senha = etSenha.text.toString()
        val confirmarSenha = etConfirmarSenha.text.toString()

        if (nome.isEmpty()) { etNome.error = "Informe seu nome"; etNome.requestFocus(); return }
        // Duas checagens separadas pro e-mail (vazio E formato), igual à
        // Tabela 1 do capítulo de qualidade: "Não pode ficar vazio e deve
        // ter formato válido" -> "Informe um e-mail válido!". A regra em si
        // mora em Validacoes.emailValido() (Kotlin puro, sem depender do
        // Android) só pra poder ser testada com um teste unitário comum —
        // ver mobile/src/test/.../util/ValidacoesTest.kt.
        if (email.isEmpty()) { etEmail.error = "Informe seu e-mail"; etEmail.requestFocus(); return }
        if (!Validacoes.emailValido(email)) {
            etEmail.error = "Informe um e-mail válido!"
            etEmail.requestFocus()
            return
        }
        if (senha.isEmpty()) { etSenha.error = "Informe uma senha"; etSenha.requestFocus(); return }
        if (senha != confirmarSenha) {
            etConfirmarSenha.error = "As senhas não coincidem"
            etConfirmarSenha.requestFocus()
            return
        }

        // parse() da SimpleDateFormat lança exceção se o texto não bater
        // com o padrão dd/MM/yyyy — por isso o try/catch, em vez de deixar
        // o app quebrar quando a pessoa digitar uma data incompleta.
        val dataNascimento: Long? = if (dataTexto.isEmpty()) {
            null
        } else {
            try {
                formatoData.parse(dataTexto)?.time
            } catch (e: java.text.ParseException) {
                etDataNascimento.error = "Use o formato dd/mm/aaaa"
                etDataNascimento.requestFocus()
                return
            }
        }

        btnCadastrar.isEnabled = false
        lifecycleScope.launch {
            val usuario = repository.cadastrar(
                nome = nome,
                dataNascimento = dataNascimento,
                telefone = telefone.ifBlank { null },
                email = email,
                deficiencia = deficiencia.ifBlank { null },
                nivelAutonomia = nivelAutonomia.ifBlank { null },
                senha = senha,
            )
            btnCadastrar.isEnabled = true

            if (usuario == null) {
                Toast.makeText(
                    this@CadastroActivity,
                    "Já existe um cadastro com esse e-mail",
                    Toast.LENGTH_SHORT,
                ).show()
                return@launch
            }

            Toast.makeText(
                this@CadastroActivity,
                "Cadastro realizado! Aproxime o relógio para sincronizar.",
                Toast.LENGTH_LONG,
            ).show()
            finish() // volta para a tela de login, já com o e-mail cadastrado
        }
    }
}
