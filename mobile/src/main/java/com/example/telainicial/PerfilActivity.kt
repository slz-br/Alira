package com.example.telainicial

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import android.view.View
import com.example.telainicial.data.repository.AliraRepository
import com.example.telainicial.data.repository.DadosPerfil
import com.example.telainicial.util.Deficiencias
import com.example.telainicial.util.Validacoes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class PerfilActivity : BaseActivity() {

    override val tituloTela = "Tela de perfil"

    private lateinit var drawer: DrawerLayout
    private lateinit var imgAvatar: ImageView
    private lateinit var campos: Map<String, EditText>
    private lateinit var campoDeficiencia: EditText

    // Códigos das deficiências marcadas (ex.: ["AUDITIVA", "VISUAL"]).
    private var deficiencias: List<String> = emptyList()

    private val prefs by lazy { getSharedPreferences("alira_perfil", Context.MODE_PRIVATE) }

    // Abre a galeria para escolher a foto de perfil
    private val escolherFoto = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) {
                // sem permissão persistente: a foto vale só nesta sessão
            }
            mostrarFoto(uri)
            prefs.edit().putString("foto", uri.toString()).apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        drawer = findViewById(R.id.main)
        imgAvatar = findViewById(R.id.img_avatar)

        // ===================== MENU =====================
        findViewById<View>(R.id.btn_menu).setOnClickListener {
            drawer.openDrawer(GravityCompat.START)
        }
        Navegacao.configurarMenu(this) // Perfil, Acessibilidade e logo -> tela inicial

        // ===================== FOTO =====================
        val abrirGaleria = View.OnClickListener { escolherFoto.launch(arrayOf("image/*")) }
        findViewById<View>(R.id.btn_camera).setOnClickListener(abrirGaleria)
        imgAvatar.setOnClickListener(abrirGaleria)

        prefs.getString("foto", null)?.let {
            try {
                mostrarFoto(Uri.parse(it))
            } catch (e: Exception) {
                prefs.edit().remove("foto").apply() // foto não existe mais
            }
        }

        // ===================== CAMPOS =====================
        campos = mapOf(
            "username" to findViewById(R.id.et_username),
            "email" to findViewById(R.id.et_email),
            "telefone" to findViewById(R.id.et_telefone),
            "genero" to findViewById(R.id.et_genero),
            "nascimento" to findViewById(R.id.et_nascimento),
            // "telefone_adicional" agora é o telefone do cuidador (o nome da
            // chave ficou igual para não perder o que já estava salvo)
            "telefone_adicional" to findViewById(R.id.et_telefone_adicional),
            "cuidador_nome" to findViewById(R.id.et_cuidador_nome),
            "cuidador_email" to findViewById(R.id.et_cuidador_email),
            "cuidador_parentesco" to findViewById(R.id.et_cuidador_parentesco)
        )

        // máscaras
        campos["nascimento"]!!.addTextChangedListener(MascaraData(campos["nascimento"]!!))
        campos["telefone"]!!.addTextChangedListener(PhoneNumberFormattingTextWatcher("BR"))
        campos["telefone_adicional"]!!.addTextChangedListener(PhoneNumberFormattingTextWatcher("BR"))

        // restaura o que foi digitado antes
        campos.forEach { (chave, campo) -> campo.setText(prefs.getString(chave, "")) }

        // ===================== DEFICIÊNCIA =====================
        // Não é digitada: tocar abre a lista de opções. Guarda os CÓDIGOS
        // (ex.: "AUDITIVA,VISUAL"), que é o que o relógio entende.
        campoDeficiencia = findViewById(R.id.et_deficiencia)
        deficiencias = Deficiencias.separar(prefs.getString("deficiencia", null))
        mostrarDeficiencias()
        campoDeficiencia.setOnClickListener { escolherDeficiencias() }
    }

    /** Abre a lista com caixinhas; a pessoa pode marcar mais de uma. */
    private fun escolherDeficiencias() {
        val codigos = Deficiencias.OPCOES.keys.toList()
        val nomes = Deficiencias.OPCOES.values.toTypedArray()
        val marcadas = BooleanArray(codigos.size) { codigos[it] in deficiencias }

        LeitorDeTela.anunciar(this, "Escolha as deficiências. Pode marcar mais de uma.")
        AlertDialog.Builder(this)
            .setTitle("Deficiência")
            .setMultiChoiceItems(nomes, marcadas) { _, posicao, marcada -> marcadas[posicao] = marcada }
            .setPositiveButton("OK") { _, _ ->
                deficiencias = codigos.filterIndexed { i, _ -> marcadas[i] }
                mostrarDeficiencias()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDeficiencias() {
        val texto = Deficiencias.nomes(Deficiencias.juntar(deficiencias))
        campoDeficiencia.setText(texto)
    }

    /**
     * Salva tudo automaticamente quando o usuário sai da tela:
     *  1. nas preferências do celular (para os campos voltarem preenchidos);
     *  2. no banco (usuário + cuidador), que é de onde sai o SMS de emergência;
     *  3. e manda uma cópia para o relógio (feito pelo AliraRepository).
     */
    override fun onPause() {
        super.onPause()
        val editor = prefs.edit()
        campos.forEach { (chave, campo) -> editor.putString(chave, campo.text.toString().trim()) }
        editor.putString("deficiencia", Deficiencias.juntar(deficiencias) ?: "")
        editor.apply()

        val dados = montarDadosPerfil()
        val nadaPreenchido = campos.values.all { it.text.isBlank() } && deficiencias.isEmpty()
        if (dados.email.isBlank() && !nadaPreenchido) {
            Toast.makeText(this, "Preencha o e-mail para o perfil ir para o relógio.", Toast.LENGTH_LONG).show()
        } else if (dados.email.isNotBlank() && !Validacoes.emailValido(dados.email)) {
            Toast.makeText(this, "Confira o e-mail: ele parece estar errado.", Toast.LENGTH_LONG).show()
        }

        // Escopo próprio (e não lifecycleScope): a tela pode estar fechando
        // agora, e o salvamento precisa terminar mesmo assim.
        val app = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            AliraRepository.getInstance(app).salvarPerfil(dados)
        }
    }

    private fun texto(chave: String): String = campos.getValue(chave).text.toString().trim()

    private fun montarDadosPerfil() = DadosPerfil(
        nome = texto("username"),
        email = texto("email"),
        telefone = texto("telefone").ifBlank { null },
        dataNascimento = lerData(texto("nascimento")),
        deficiencia = Deficiencias.juntar(deficiencias),
        cuidadorNome = texto("cuidador_nome"),
        cuidadorTelefone = texto("telefone_adicional"),
        cuidadorEmail = texto("cuidador_email").ifBlank { null },
        cuidadorParentesco = texto("cuidador_parentesco").ifBlank { null },
    )

    /** "25/12/2000" -> milissegundos (o formato que o banco guarda); null se a data for inválida. */
    private fun lerData(texto: String): Long? = try {
        SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).apply { isLenient = false }.parse(texto)?.time
    } catch (e: Exception) {
        null
    }

    private fun mostrarFoto(uri: Uri) {
        imgAvatar.setImageURI(uri)
        findViewById<ImageView>(R.id.menu_avatar)?.setImageURI(uri) // mesma foto no menu lateral
    }

    /** Formata a data enquanto digita: 25122000 -> 25/12/2000 */
    private class MascaraData(private val campo: EditText) : TextWatcher {
        private var atualizando = false

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            if (atualizando) return
            val digitos = s.toString().filter { it.isDigit() }.take(8)
            val formatado = StringBuilder()
            digitos.forEachIndexed { i, c ->
                if (i == 2 || i == 4) formatado.append('/')
                formatado.append(c)
            }
            atualizando = true
            campo.setText(formatado)
            campo.setSelection(formatado.length)
            atualizando = false
        }
    }
}
