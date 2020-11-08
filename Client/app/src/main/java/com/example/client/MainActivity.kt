package com.example.client

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {
    lateinit var db: FirebaseFirestore
    lateinit var notificationsCollectionRef: CollectionReference
    lateinit var notificationAdapter: NotificationAdapter
    private lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this.applicationContext

        FirebaseApp.initializeApp(this)
        db = FirebaseFirestore.getInstance()
        notificationsCollectionRef = db.collection("Notifications")

        setContentView(R.layout.activity_main)

        setUpRecyclerView();
    }

    private fun setUpRecyclerView() {
        val query: Query = notificationsCollectionRef.orderBy("time", Query.Direction.DESCENDING)

        val options: FirestoreRecyclerOptions<Notification> =
            FirestoreRecyclerOptions.Builder<Notification>()
                .setQuery(query, Notification::class.java).build()

        notificationAdapter = NotificationAdapter(options, context)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.setHasFixedSize(true)
        recyclerView.setLayoutManager(LinearLayoutManager(this))
        recyclerView.adapter = notificationAdapter

        notificationAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        notificationAdapter.stopListening()
    }
}
