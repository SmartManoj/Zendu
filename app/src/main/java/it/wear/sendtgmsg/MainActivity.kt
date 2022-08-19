package it.wear.sendtgmsg

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import it.wear.sendtgmsg.databinding.ActivityMainBinding
import it.wear.libsgram.TelegramConfiguration
import it.tdlight.client.SimpleTelegramClient


class MainActivity : FragmentActivity(), View.OnClickListener {

    companion object {
        const val PREFERENCE_NAME = "SEND_TELEGRAM_PREFS"
        const val PHONE_NUMBER_KEY = "PHONE_NUMBER"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var client: SimpleTelegramClient
    private lateinit var storedPhoneNumber: String
    private var inputPhoneNumber : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Crete layout
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve stored phone number
        val sharedPreference =  getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        storedPhoneNumber = sharedPreference.getString(PHONE_NUMBER_KEY, "").toString()

        if (storedPhoneNumber != "") {
            try {
                // Try to create client
                if (TelegramConfiguration.getInstance().client == null) {
                    client = ClientFactory.buildClient(
                        applicationContext,
                        Secrets.API_ID,
                        Secrets.API_HASH,
                        storedPhoneNumber
                    )
                    Thread.sleep(500)

                    TelegramConfiguration.getInstance().client = client
                    TelegramConfiguration.getInstance().authSemaphore.acquire()
                    TelegramConfiguration.getInstance().loggedStatusSemaphore.acquire()
                }

                if (TelegramConfiguration.getInstance().isNeedLogin) {
                    // Clear phone number
                    val sharedPreferenceEditor = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit()
                    sharedPreferenceEditor.remove(PHONE_NUMBER_KEY)
                    sharedPreferenceEditor.commit()

                    val frag: Fragment = Fragment(R.layout.fragment_phone)
                    val fm = supportFragmentManager.beginTransaction()
                    fm.replace(R.id.mainLayout, frag)
                    fm.commit()
                } else {
                    TelegramConfiguration.getInstance().loggedStatusSemaphore.release()
                    val intent: Intent = Intent(this, ChatListActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Cannot Start Telegram", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            val frag: Fragment = Fragment(R.layout.fragment_phone)
            val fm = supportFragmentManager.beginTransaction()
            fm.replace(R.id.mainLayout, frag)
            fm.commit()
        }
    }

    override fun onClick(v: View?) {
        Log.d("MainActivity", "Clicked")
        when (v?.id) {
            R.id.welcomeBtn -> {
                val frag: Fragment = Fragment(R.layout.fragment_phone)
                val fm = supportFragmentManager.beginTransaction()
                fm.replace(R.id.mainLayout, frag)
                fm.commit()
            }
            R.id.startBtn -> {
                try {
                    val phoneEditText = findViewById<EditText>(R.id.editTextPhone)
                    inputPhoneNumber = phoneEditText.text.toString()
                    client = ClientFactory.buildClient(
                        applicationContext,
                        Secrets.API_ID,
                        Secrets.API_HASH,
                        inputPhoneNumber
                    )
                    Thread.sleep(500)
//                    Toast.makeText(applicationContext, "Telegram Started", Toast.LENGTH_SHORT)
//                        .show()

                    TelegramConfiguration.getInstance().client = client
                    TelegramConfiguration.getInstance().authSemaphore.acquire()
                    TelegramConfiguration.getInstance().loggedStatusSemaphore.acquire()
                    if(TelegramConfiguration.getInstance().isNeedLogin) {
                        val frag: Fragment = Fragment(R.layout.fragment_code)
                        val fm = supportFragmentManager.beginTransaction()
                        fm.replace(R.id.mainLayout, frag)
                        fm.commit()
                    } else {
                        Toast.makeText(applicationContext, "No Need To Login", Toast.LENGTH_SHORT)
                            .show()
                        TelegramConfiguration.getInstance().loggedStatusSemaphore.release()

                        // Save phone number for later
                        val sharedPreferenceEditor = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit()
                        sharedPreferenceEditor.putString(PHONE_NUMBER_KEY, inputPhoneNumber)
                        sharedPreferenceEditor.commit()

                        val intent: Intent = Intent(this, ChatListActivity::class.java)
                        startActivity(intent)
                        finish()
                    }

                } catch (e: Exception) {
                    Toast.makeText(applicationContext, "Cannot Start Telegram", Toast.LENGTH_SHORT)
                        .show()
                    e.message?.let { it1 -> Log.e("MainActivity", it1) }
                }
            }
            R.id.sencCodeBtn -> {
                val codeEditText = findViewById<EditText>(R.id.editTextCode)
                TelegramConfiguration.getInstance().authCode = codeEditText.text.toString()
                Toast.makeText(applicationContext, "Telegram Started", Toast.LENGTH_SHORT)
                    .show()
                TelegramConfiguration.getInstance().authSemaphore.release()

                // Save phone number for later
                val sharedPreferenceEditor = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit()
                sharedPreferenceEditor.putString(PHONE_NUMBER_KEY, inputPhoneNumber)
                sharedPreferenceEditor.commit()

                TelegramConfiguration.getInstance().loggedStatusSemaphore.release()
                val intent: Intent = Intent(this, ChatListActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}