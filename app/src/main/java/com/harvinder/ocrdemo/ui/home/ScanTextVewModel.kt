package com.harvinder.ocrdemo.ui.home

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.harvinder.ocrdemo.constants.Constants
import com.harvinder.ocrdemo.model.ScanText

class ScanTextVewModel(
    private val rootRef: DatabaseReference = FirebaseDatabase.getInstance().reference,
    private val scanRef: DatabaseReference = rootRef.child(Constants.SCAN_REF)
) : ViewModel() {
    var scanTextList: MutableLiveData<ScanText?>? = null


    init {

    }

    fun getScanListData() {
        scanTextList = MutableLiveData()
        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get Post object and use the values to update the UI
                if(dataSnapshot.value == null){
                    scanTextList?.value = null
                }else{
                    val post = dataSnapshot.children
                    post.forEach {
                        println(it.value)
                        it.child("txt").value
                        val scanText = ScanText(it.child("txt").value.toString())
                        scanTextList?.value = scanText
                    }
                }




               // Log.w(Constants.TAG, "" + post)

            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                scanTextList?.value = null
                Log.w(Constants.TAG, "loadPost:onCancelled", databaseError.toException())
            }
        }
        scanRef!!.addValueEventListener(postListener)
    }

}