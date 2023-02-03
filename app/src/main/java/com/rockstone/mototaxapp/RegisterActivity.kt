package com.rockstone.mototaxapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import com.rockstone.mototaxapp.databinding.ActivityMainBinding
import com.rockstone.mototaxapp.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding:ActivityRegisterBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        binding.btnGoToLogin.setOnClickListener { goToLogin() }
        binding.btnRegister.setOnClickListener { register() }
    }

     private fun register(){
         val nombre = binding.textFieldName.text.toString()
         val apellidos = binding.textFieldLastName.text.toString()
         val telefono = binding.textFieldPhone.text.toString()
         val email = binding.textFieldEmail.text.toString()
         val password = binding.textFieldPassword.text.toString()
         val confirmPassword = binding.textFieldConfirmPassword.text.toString()

         if(isValidForm(nombre,apellidos,telefono,email,password,confirmPassword)) {
             Toast.makeText(this, "Registro Exitoso", Toast.LENGTH_SHORT).show()
         }
     }

     private  fun isValidForm(nombre:String,apellidos:String,telefono:String,email:String,password:String,confirmPassword:String):Boolean {
          if(nombre.isEmpty()) {
              Toast.makeText(this, "Debes Ingresar nombre", Toast.LENGTH_SHORT).show()
              return false
          }
         if(apellidos.isEmpty()) {
             Toast.makeText(this, "Debes Ingresar Apellidos", Toast.LENGTH_SHORT).show()
             return false
         }
         if(telefono.isEmpty()) {
             Toast.makeText(this, "Debes Ingresar Telefono", Toast.LENGTH_SHORT).show()
             return false
         }
         if(email.isEmpty()) {
             Toast.makeText(this, "Debes Ingresar Email", Toast.LENGTH_SHORT).show()
             return false
         }
         if(password.length <6) {
             Toast.makeText(this, "Password debe tener minimo 5 caracteres", Toast.LENGTH_SHORT).show()
             return false
         }
         if(confirmPassword.length < 6) {
             Toast.makeText(this, "Debes Ingresar Confirmacion", Toast.LENGTH_SHORT).show()
             return false
         }
         if(password != confirmPassword) {
             Toast.makeText(this, "Las Contraseñas no coinciden!!", Toast.LENGTH_SHORT).show()
             return false
         }
         return true
     }

    private fun goToLogin(){
        val i = Intent(this,MainActivity::class.java)
        startActivity(i)
    }

}