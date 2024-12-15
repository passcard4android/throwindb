package me.hda.throwindb

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import me.hda.throwindb.NewQRCodeActivity.Companion.QRCODE_DATA
import me.hda.throwindb.extra.QRCodeListAdapter
import me.hda.throwindb.extra.QRCodeListDatasource

class QRCodeListActivity : AppCompatActivity() {
    private val listAdapter = QRCodeListAdapter {
        val intent = Intent(this@QRCodeListActivity, QRCodeDetailActivity::class.java)
        intent.putExtra(QRCodeDetailActivity.QRCODE_DATA, it.data)
        intent.putExtra(QRCodeDetailActivity.QRCODE_TIMESTAMP, it.timestamp.toEpochSecond())
        startActivity(intent)
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: View
    private lateinit var newQRCodeActivity: ActivityResultLauncher<Intent>
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        fab = findViewById(R.id.fab)
        recyclerView = findViewById(R.id.recycler_view)
        searchView = findViewById(R.id.search_view)

        QRCodeListDatasource.fromContext(this)
        recyclerView.adapter = listAdapter

        fab.setOnClickListener {
            startNewQRCodeActivity()
        }

        // Set up search query listener
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                listAdapter.filter(newText)  // Call the filter function in the adapter
                return true
            }
        })

        newQRCodeActivity = registerForActivityResult(StartActivityForResult()) {
            if (it.data != null) {
                val result = it.resultCode
                if (result == RESULT_OK) {
                    val qrCodeData = it.data?.getStringExtra(QRCODE_DATA)
                    if (qrCodeData != null) {
                        QRCodeListDatasource.add(qrCodeData)
                    } else {
                        Toast.makeText(this@QRCodeListActivity, R.string.new_qrcode_error, Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@QRCodeListActivity, R.string.new_qrcode_error, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchItem: MenuItem? = menu?.findItem(R.id.action_search)
        val searchView: SearchView = searchItem?.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Handle query submission, if needed
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Pass the query to the adapter to filter
                listAdapter.filter(newText)
                return true
            }
        })

        return true
    }


    override fun onResume() {
        super.onResume()
        QRCodeListDatasource.liveData.observe(this) {
            if (it != null) {
                listAdapter.setFullList(it)
            }
        }
    }


    override fun onStop() {
        super.onStop()
        QRCodeListDatasource.liveData.removeObservers(this)
    }

    private fun startNewQRCodeActivity() {
        val intent = Intent(this, NewQRCodeActivity::class.java)
        newQRCodeActivity.launch(intent)
    }

    fun startScanActivity(item: MenuItem) {
        val intent = Intent(this, QRCodeScanActivity::class.java)
        newQRCodeActivity.launch(intent)
    }
}
