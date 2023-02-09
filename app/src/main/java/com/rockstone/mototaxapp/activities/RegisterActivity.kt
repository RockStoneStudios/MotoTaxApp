package com.rockstone.mototaxapp.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import com.rockstone.mototaxapp.databinding.ActivityRegisterBinding
import com.rockstone.mototaxapp.models.Client
import com.rockstone.mototaxapp.providers.AuthProvider
import com.rockstone.mototaxapp.providers.ClientProvider

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding:ActivityRegisterBinding
    private val authProvider = AuthProvider()
    private val clientProvider = ClientProvider()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        binding.btnGoToLogin.setOnClickListener { goToLogin() }
        binding.btnRegister.setOnClickListener { register() }
    }

     private fun register(){
         val name = binding.textFieldName.text.toString()
         val lastname = binding.textFieldLastName.text.toString()
         val phone = binding.textFieldPhone.text.toString()
         val email = binding.textFieldEmail.text.toString()
         val password = binding.textFieldPassword.text.toString()
         val confirmPassword = binding.textFieldConfirmPassword.text.toString()

         if(isValidForm(name,lastname,phone,email,password,confirmPassword)) {
             authProvider.register(email,password).addOnCompleteListener { 
                  if(it.isSuccessful) {
                      val client = Client(
                          id = authProvider.getId(),
                          name = name,
                          lastname = lastname,
                          phone = phone,
                          email = email
                      )
                      clientProvider.create(client).addOnCompleteListener { 
                          if(it.isSuccessful){
                              Toast.makeText(this@RegisterActivity, "Registro Exitoso", Toast.LENGTH_LONG).show()
                              goToMap()
                          }else {
                              Toast.makeText(this@RegisterActivity, "Error almacenando datos ${it.exception.toString()}", Toast.LENGTH_LONG).show()
                          }
                      }

                  }else {
                      Toast.makeText(this@RegisterActivity, "Registro Fallido ${it.exception.toString()}", Toast.LENGTH_LONG).show()
                  }
             }
         }
     }
    private fun goToMap(){
        val i = Intent(this,MapActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
    }
     private  fun isValidForm(name:String,lastname:String,phone:String,email:String,password:String,confirmPassword:String):Boolean {
          if(name.isEmpty()) {
              Toast.makeText(this, "Debes Ingresar nombre", Toast.LENGTH_SHORT).show()
              return false
          }
         if(lastname.isEmpty()) {
             Toast.makeText(this, "Debes Ingresar Apellidos", Toast.LENGTH_SHORT).show()
             return false
         }
         if(phone.isEmpty()) {
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
        val i = Intent(this, MainActivity::class.java)
        startActivity(i)
    }

}