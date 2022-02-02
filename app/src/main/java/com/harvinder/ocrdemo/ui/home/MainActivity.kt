package com.harvinder.ocrdemo.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.harvinder.ocrdemo.R
import com.harvinder.ocrdemo.constants.Constants
import com.harvinder.ocrdemo.databinding.ActivityMainBinding
import com.harvinder.ocrdemo.model.ScanText
import com.harvinder.ocrdemo.ui.scan.CameraViewActivity






class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: ScanTextVewModel
    var databaseReference: DatabaseReference? = null
    private lateinit var scanDataAdapter: ScanDataAdapter
    lateinit var binding: ActivityMainBinding
    private lateinit var scandataList: ArrayList<ScanText>
    private val neededPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    lateinit var tv_txt: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(MainActivity@ this).get(ScanTextVewModel::class.java)
        databaseReference = FirebaseDatabase.getInstance().reference.child(Constants.SCAN_REF)
        val result = checkPermission()
        if (result) {

        }
        scanDataAdapter = ScanDataAdapter()
        binding.rvScan?.layoutManager = LinearLayoutManager(this)
        binding.rvScan?.adapter = scanDataAdapter

        binding.swipeLayout.setOnRefreshListener {
            binding.swipeLayout.isRefreshing=false
            getResponseUsing()
        }
    }

    private fun checkPermission(): Boolean {
        val currentAPIVersion = Build.VERSION.SDK_INT
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            val permissionsNotGranted = ArrayList<String>()
            for (permission in neededPermissions) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionsNotGranted.add(permission)
                }
            }
            if (permissionsNotGranted.size > 0) {
                var shouldShowAlert = false
                for (permission in permissionsNotGranted) {
                    shouldShowAlert =
                        ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
                }

                val arr = arrayOfNulls<String>(permissionsNotGranted.size)
                val permissions = permissionsNotGranted.toArray(arr)
                if (shouldShowAlert) {
                    showPermissionAlert(permissions)
                } else {
                    requestPermissions(permissions)
                }
                return false
            }
        }
        return true
    }

    private fun showPermissionAlert(permissions: Array<String?>) {
        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setCancelable(true)
        alertBuilder.setTitle(R.string.permission_required)
        alertBuilder.setMessage(R.string.permission_message)
        alertBuilder.setPositiveButton(R.string.yes) { _, _ -> requestPermissions(permissions) }
        val alert = alertBuilder.create()
        alert.show()
    }

    private fun requestPermissions(permissions: Array<String?>) {
        ActivityCompat.requestPermissions(this@MainActivity, permissions, REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE -> {
                for (result in grantResults) {
                    if (result == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(
                            this@MainActivity,
                            R.string.permission_warning,
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }


                }
            }

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        const val REQUEST_CODE = 100
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_button, menu)
        val searchItem = menu!!.findItem(R.id.actionSearch)
        val searchView: SearchView = searchItem.getActionView() as SearchView

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    filter(newText)
                }
                return false;
            }

        })

       return true
    }

    fun filter(str:String){
        val filteredlist: ArrayList<ScanText> = ArrayList()
        for (item in scandataList) {
            // checking if the entered string matched with any item of our recycler view.
            if (item.txt?.toLowerCase()!!.contains(str.toLowerCase())) {
                // if the item is matched we are
                // adding it to our filtered list.
                filteredlist.add(item)
            }
        }
        if (filteredlist.isEmpty()) {
            // if no item is added in filtered list we are
            // displaying a toast message as no data found.
            Toast.makeText(this, "No Data Found..", Toast.LENGTH_SHORT).show()
        } else {
            // at last we are passing that filtered
            // list to our adapter class.
            scanDataAdapter.submitList(filteredlist)
            scanDataAdapter.notifyDataSetChanged()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.next_view -> {
                startActivity(
                    Intent(this@MainActivity, CameraViewActivity::class.java)
                )
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        getResponseUsing()
    }

    private fun getResponseUsing() {
        binding.progress.visibility = View.VISIBLE
        binding.rvScan.visibility = View.GONE
        scandataList = ArrayList()
        viewModel.getScanListData()
        viewModel.scanTextList?.observe(this, {
            try {
                binding.progress.visibility = View.GONE
                if (it != null) {
                    binding.rvScan.visibility = View.VISIBLE
                    scandataList.add(it)
                }
                scanDataAdapter.submitList(
                    scandataList)
                scanDataAdapter.notifyDataSetChanged()


            } catch (e: Exception) {
                binding.progress.visibility = View.GONE
                e.printStackTrace()
            }


        })
    }


}