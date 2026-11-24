package com.example.telainicial

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

/**
 * Liga as navegações comuns a todas as telas:
 *  - menu_perfil        -> PerfilActivity
 *  - menu_inicio ("Menu") -> tela inicial (MainActivity)
 *  - menu_avatar        -> mostra a foto de perfil do usuário; ao tocar, abre o Perfil
 *  - menu_acessibilidade -> AcessibilidadeActivity
 *  - menu_relatorios    -> BatimentosActivity (gráficos com os dados do relógio)
 *  - logo ALIRA         -> volta para a tela inicial (MainActivity)
 *
 * Use no onCreate, depois do setContentView:  Navegacao.configurarMenu(this)
 */
object Navegacao {

    fun configurarMenu(activity: AppCompatActivity) {
        val drawer = activity.findViewById<DrawerLayout>(R.id.main)

        irPara(activity, drawer, R.id.menu_perfil, PerfilActivity::class.java)
        irParaInicio(activity, drawer, R.id.menu_inicio)
        irPara(activity, drawer, R.id.menu_acessibilidade, AcessibilidadeActivity::class.java)
        irPara(activity, drawer, R.id.menu_relatorios, BatimentosActivity::class.java)

        // Foto do menu = foto do perfil; tocar nela abre o Perfil
        atualizarAvatar(activity)
        irPara(activity, drawer, R.id.menu_avatar, PerfilActivity::class.java)

        // Logo: na tela inicial o id é "imageView3"; na de batimentos é "logo"
        val logo = activity.findViewById<View>(R.id.imageView3) ?: activity.findViewById<View>(R.id.logo)
        logo?.setOnClickListener { voltarParaInicio(activity, drawer) }
    }

    /**
     * Coloca no menu lateral a MESMA foto que o usuário escolheu em Perfil
     * (guardada em "alira_perfil" > "foto"). Sem foto, fica o ícone padrão.
     */
    fun atualizarAvatar(activity: AppCompatActivity) {
        val avatar = activity.findViewById<ImageView>(R.id.menu_avatar) ?: return
        val uri = activity.getSharedPreferences("alira_perfil", Context.MODE_PRIVATE).getString("foto", null)

        if (uri == null) {
            avatar.setImageResource(R.drawable.ic_person)
            return
        }
        try {
            avatar.setImageURI(Uri.parse(uri))
        } catch (e: Exception) {
            avatar.setImageResource(R.drawable.ic_person) // foto não existe mais
        }
    }

    private fun voltarParaInicio(activity: AppCompatActivity, drawer: DrawerLayout?) {
        drawer?.closeDrawer(GravityCompat.START)
        if (activity !is MainActivity) {
            val intent = Intent(activity, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            activity.startActivity(intent)
        }
    }

    private fun irParaInicio(activity: AppCompatActivity, drawer: DrawerLayout?, idItem: Int) {
        activity.findViewById<View>(idItem)?.setOnClickListener { voltarParaInicio(activity, drawer) }
    }

    private fun irPara(
        activity: AppCompatActivity,
        drawer: DrawerLayout?,
        idItem: Int,
        destino: Class<*>
    ) {
        activity.findViewById<View>(idItem)?.setOnClickListener {
            drawer?.closeDrawer(GravityCompat.START)
            if (activity.javaClass != destino) {
                activity.startActivity(Intent(activity, destino))
            }
        }
    }
}
