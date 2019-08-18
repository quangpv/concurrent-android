package ps.billyphan.concurrent

import android.annotation.SuppressLint
import androidx.lifecycle.Observer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModel()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel.result.observe(this, Observer {
            txtUser.text = "${it!!.first} - ${it.second} - ${it.third}"
        })

        viewModel.loading.observe(this, Observer {
            btnTest.text = if (it!!) "loading..." else ""
        })

        viewModel.error.observe(this, Observer {
            Toast.makeText(this, it?.message ?: "Unknown", Toast.LENGTH_SHORT).show()
        })
        btnTest.setOnClickListener {
            viewModel.loadUsers()
        }
    }
}
