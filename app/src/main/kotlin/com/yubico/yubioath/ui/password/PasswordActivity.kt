package com.yubico.yubioath.ui.password

import android.app.Activity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import com.yubico.yubioath.R
import com.yubico.yubioath.ui.BaseActivity
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

class PasswordActivity : BaseActivity<PasswordViewModel>(PasswordViewModel::class.java) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password)

        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_close_24dp)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_password, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.menu_password_save -> (supportFragmentManager.findFragmentById(R.id.fragment) as PasswordFragment).apply {
                val data = validateData()
                if (data != null) {
                    val job = viewModel.setPassword(data.current_password, data.new_password, data.remember).apply {
                        invokeOnCompletion {
                            launch(UI) {
                                if (!isCancelled) {
                                    val success = await()
                                    if (success) {
                                        setResult(Activity.RESULT_OK)
                                        finish()
                                    } else {
                                        setWrongPassword()
                                    }
                                }
                            }
                        }
                    }
                    launch(UI) {
                        if (viewModel.lastDeviceInfo.persistent) {
                            delay(100)
                        }
                        if (job.isActive) {
                            Snackbar.make(view!!, R.string.swipe_and_hold, Snackbar.LENGTH_INDEFINITE).apply {
                                setActionTextColor(ContextCompat.getColor(context, R.color.yubicoPrimaryGreen))
                                setAction(R.string.cancel) {
                                    job.cancel()
                                }
                                job.invokeOnCompletion {
                                    launch(UI) {
                                        dismiss()
                                    }
                                }
                            }.show()
                        }
                    }
                }
            }
        }
        return true
    }
}