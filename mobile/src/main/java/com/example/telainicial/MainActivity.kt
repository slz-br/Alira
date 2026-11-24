package com.example.telainicial

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout

class MainActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override val tituloTela = "Tela inicial"

    // Pede a permissão de SMS: sem ela o celular não consegue avisar o
    // cuidador quando o relógio detecta uma queda ou crise
    // (ver data/sync/MobileDataLayerService.kt).
    private val pedidoDePermissaoSms =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
            if (!concedida) {
                Toast.makeText(
                    this,
                    "Sem a permissão de SMS, o Alira não consegue avisar seu cuidador.",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupDrawerMenu()
        pedirPermissaoDeSmsSeNecessario()
    }

    private fun pedirPermissaoDeSmsSeNecessario() {
        val permissao = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
        if (permissao != PackageManager.PERMISSION_GRANTED) {
            pedidoDePermissaoSms.launch(Manifest.permission.SEND_SMS)
        }
    }

    private fun setupDrawerMenu() {
        drawerLayout = findViewById(R.id.main)

        // Abre o menu ao tocar no ícone de hambúrguer
        findViewById<ImageView>(R.id.btn_menu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Fecha o menu e executa a ação do item
        fun menuItem(viewId: Int, action: () -> Unit) {
            findViewById<View>(viewId).setOnClickListener {
                drawerLayout.closeDrawer(GravityCompat.START)
                action()
            }
        }

        // Troque os Toasts pelas suas telas, ex.:
        // startActivity(Intent(this, PerfilActivity::class.java))
        menuItem(R.id.menu_institucional)  { Toast.makeText(this, "Institucional", Toast.LENGTH_SHORT).show() }

        // Perfil, Acessibilidade, Relatórios e logo ALIRA (-> tela inicial)
        Navegacao.configurarMenu(this)

        // Botões da tela inicial
        // "Relatórios" abre a tela de Batimentos, com os gráficos dos dados do relógio.
        findViewById<View>(R.id.rela_buttonback).setOnClickListener {
            startActivity(Intent(this, BatimentosActivity::class.java))
        }
        findViewById<View>(R.id.bat_button).setOnClickListener {
            startActivity(Intent(this, BatimentosActivity::class.java))
        }
        findViewById<View>(R.id.image_button).setOnClickListener {
            startActivity(Intent(this, LeitorImagemActivity::class.java))
        }
        // "Leitor de tela": liga/desliga o leitor (o mesmo botão ON/OFF da tela de Acessibilidade)
        findViewById<View>(R.id.leitor_button).setOnClickListener { alternarLeitor() }
        atualizarBotaoLeitor()

        // Botão "voltar" fecha o menu se ele estiver aberto
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    // ---------------------------------------------------------------- LEITOR DE TELA
    private fun alternarLeitor() {
        val ligar = !AcessibilidadeManager.leitorLigado(this)
        AcessibilidadeManager.setLeitorLigado(this, ligar)
        atualizarBotaoLeitor()

        val aviso = if (ligar) {
            "Leitor de tela ativado. Toque uma vez para ouvir e toque de novo para ativar."
        } else {
            "Leitor de tela desativado."
        }
        LeitorDeTela.falarSempre(this, aviso)
        Toast.makeText(this, aviso, Toast.LENGTH_SHORT).show()
    }

    private fun atualizarBotaoLeitor() {
        val estado = if (AcessibilidadeManager.leitorLigado(this)) "ligado" else "desligado"
        findViewById<View>(R.id.leitor_button).contentDescription = "Leitor de tela, $estado"
    }

    // volta de Acessibilidade -> o estado pode ter mudado
    override fun aoAplicarAcessibilidade() = atualizarBotaoLeitor()
}
