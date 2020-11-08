package com.example.client

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.squareup.picasso.Picasso
import java.io.IOException

private const val LOG_TAG = "CLIENT_TAG"

class NotificationAdapter(options: FirestoreRecyclerOptions<Notification>,
                          private var context: Context
) :
    FirestoreRecyclerAdapter<Notification, NotificationAdapter.NotificationHolder>(options) {

    private var player: MediaPlayer? = null

    inner class NotificationHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView = itemView.findViewById(R.id.image_view)
        var time: TextView = itemView.findViewById(R.id.time)
        var decibels: TextView = itemView.findViewById(R.id.decibels)
        var playButton: Button = itemView.findViewById(R.id.play_sound)
        var mStartPlaying:Boolean = true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(
            R.layout.notification_item,
            parent, false
        )
        return NotificationHolder(v)
    }

    override fun onBindViewHolder(
        holder: NotificationHolder,
        position: Int,
        notification: Notification
    ) {
        holder.time.text = notification.time
        holder.decibels.text = notification.decibels.toString()
        Picasso.get().load(notification.imageUrl).into(holder.imageView)
        
        holder.playButton.setOnClickListener {
            onPlay(holder, notification.soundUrl)
            holder.playButton.text = when (holder.mStartPlaying) {
                true -> "Stop playing"
                false -> "Start playing"
            }
            holder.mStartPlaying = !holder.mStartPlaying
        }
    }

    private fun onPlay(holder: NotificationHolder, url: String) = if (holder.mStartPlaying) {
        startPlaying(holder, url)
    } else {
        stopPlaying()
    }

    private fun startPlaying(holder:NotificationHolder, url: String) {
        player = MediaPlayer().apply {
            try {
                setDataSource(url)
                setOnCompletionListener {
                    onPlay(holder, url)
                    holder.playButton.text = when (holder.mStartPlaying) {
                        true -> "Stop playing"
                        false -> "Start playing"
                    }
                    holder.mStartPlaying = !holder.mStartPlaying
                }
                prepare()
                start()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }
        }
    }

    private fun stopPlaying() {
        player?.release()
        player = null
    }
}