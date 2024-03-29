package com.vivokey.vivoauth.ui.add

import android.app.Activity
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import com.vivokey.vivoauth.R
import com.vivokey.vivoauth.ui.BaseActivity
import com.vivokey.vivoauth.exc.DuplicateKeyException
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

class AddCredentialActivity : BaseActivity<AddCredentialViewModel>(AddCredentialViewModel::class.java) {
    companion object {
        const val EXTRA_CREDENTIAL = "credential"
        const val EXTRA_CODE = "code"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_credential)

        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_close_24dp)
            setDisplayHomeAsUpEnabled(true)
        }

        intent.data?.let {
            viewModel.handleScanResults(it)
        }

        // Custom colours for progress bar
        var green = ContextCompat.getColor(this, R.color.luridGreen)
        var progressBar = findViewById<ProgressBar>(R.id.progressBar)

        progressBar.progressDrawable.setColorFilter(green, PorterDuff.Mode.SRC_IN)
        progressBar.indeterminateDrawable.setColorFilter(green, PorterDuff.Mode.SRC_IN)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_add_credential, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.menu_add_credential_save -> (supportFragmentManager.findFragmentById(R.id.fragment) as AddCredentialFragment).apply {
                val data = validateData()
                if (data != null) {
                    isEnabled = false

                    val job = viewModel.addCredential(data)
                    job.invokeOnCompletion {
                        if(job.isCompletedExceptionally) {
                            launch(UI) {
                                isEnabled = true
                            }
                        }
                    }

                    launch(UI) {
                        if (viewModel.lastDeviceInfo.persistent) {
                            delay(100)
                        }
                        if (job.isActive) {
                            Snackbar.make(view!!, R.string.swipe_and_hold, Snackbar.LENGTH_INDEFINITE).apply {
                                setActionTextColor(ContextCompat.getColor(context, R.color.primaryAccent))
                                job.invokeOnCompletion { dismiss() }
                                setAction(R.string.cancel) { job.cancel() }
                            }.show()
                        }
                        try {
                            val (credential, code) = job.await()
                            setResult(Activity.RESULT_OK, Intent().apply {
                                putExtra(EXTRA_CREDENTIAL, credential)
                                code?.let {
                                    putExtra(EXTRA_CODE, it)
                                }
                            })
                            finish()
                        } catch (e: CancellationException) {
                        } catch (e: DuplicateKeyException) {
                            markDuplicateName()
                        } catch (e: Exception) {
                            Log.e("yubioath", "exception", e)
                            validateVersion(data, viewModel.lastDeviceInfo.version)
                        }
                    }
                }
            }
        }
        return true
    }
}
