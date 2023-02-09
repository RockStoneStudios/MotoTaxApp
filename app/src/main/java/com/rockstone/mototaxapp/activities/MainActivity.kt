package com.rockstone.mototaxapp.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import com.rockstone.mototaxapp.databinding.ActivityMainBinding
import com.rockstone.mototaxapp.providers.AuthProvider


class MainActivity : AppCompatActivity() {
      private lateinit var binding:ActivityMainBinding
      private val authProvider = AuthProvider()
        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        binding.btnRegister.setOnClickListener { gotoRegister() }
        binding.btnLogin.setOnClickListener { login() }

    }
    
    private fun login() {
        val email = binding.textFieldEmail.text.toString()
        val password = binding.textFieldPassword.text.toString()
         if(isValidForm(email,password)) {
           authProvider.login(email,password).addOnCompleteListener {
               if(it.isSuccessful){
                   goToMap()
               }else{
                   Toast.makeText(this@MainActivity, "Error Iniciando Session", Toast.LENGTH_LONG).show()
               }
           }
         }


    }

    private fun goToMap(){
        val i = Intent(this,MapActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
    }

    private fun isValidForm(email:String,password:String):Boolean {
        if(email.isEmpty()) {
            Toast.makeText(this, "Ingresa tu correo Electronico", Toast.LENGTH_SHORT).show()
            return false
        }
        if(password.isEmpty() || password.length<5){
            Toast.makeText(this,"Password debe contener minimo 5 caracteres",Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun gotoRegister() {
        val i = Intent(this, RegisterActivity::class.java)

        startActivity(i)
    }

    override fun onStart() {
        super.onStart()
        if(authProvider.existSession()){
            goToMap()
        }
    }

}