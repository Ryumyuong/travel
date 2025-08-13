package com.hehe.travel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DestinationAdapter : RecyclerView.Adapter<DestinationAdapter.ViewHolder>() {

    private var destinations = listOf<Destination>()

    fun submitList(list: List<Destination>) {
        destinations = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_destination, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(destinations[position])
    }

    override fun getItemCount() = destinations.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.tv_destination_name)
        private val descText: TextView = itemView.findViewById(R.id.tv_destination_desc)
        private val categoryText: TextView = itemView.findViewById(R.id.tv_destination_category)

        fun bind(destination: Destination) {
            nameText.text = destination.name
            descText.text = destination.description
            categoryText.text = destination.category
        }
    }
}

class CuisineAdapter : RecyclerView.Adapter<CuisineAdapter.ViewHolder>() {

    private var cuisine = listOf<CuisineItem>()

    fun submitList(list: List<CuisineItem>) {
        cuisine = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cuisine, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(cuisine[position])
    }

    override fun getItemCount() = cuisine.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.tv_cuisine_name)
        private val descText: TextView = itemView.findViewById(R.id.tv_cuisine_desc)
        private val typeText: TextView = itemView.findViewById(R.id.tv_cuisine_type)

        fun bind(cuisine: CuisineItem) {
            nameText.text = cuisine.name
            descText.text = cuisine.description
            typeText.text = cuisine.type
        }
    }
}

class ItineraryAdapter : RecyclerView.Adapter<ItineraryAdapter.ViewHolder>() {

    private var itinerary = listOf<ItineraryDay>()

    fun submitList(list: List<ItineraryDay>) {
        itinerary = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_itinerary_day, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(itinerary[position])
    }

    override fun getItemCount() = itinerary.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayText: TextView = itemView.findViewById(R.id.tv_day)
        private val titleText: TextView = itemView.findViewById(R.id.tv_day_title)
        private val activitiesRecyclerView: RecyclerView = itemView.findViewById(R.id.rv_activities)
        private val activityAdapter = ActivityAdapter()

        init {
            activitiesRecyclerView.apply {
                layoutManager = LinearLayoutManager(itemView.context)
                adapter = activityAdapter
            }
        }

        fun bind(day: ItineraryDay) {
            dayText.text = "${day.day}일차"
            titleText.text = day.title
            activityAdapter.submitList(day.activities)
        }
    }
}

class ActivityAdapter : RecyclerView.Adapter<ActivityAdapter.ViewHolder>() {

    private var activities = listOf<Activity>()

    fun submitList(list: List<Activity>) {
        activities = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(activities[position])
    }

    override fun getItemCount() = activities.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeText: TextView = itemView.findViewById(R.id.tv_time)
        private val titleText: TextView = itemView.findViewById(R.id.tv_activity_title)
        private val descText: TextView = itemView.findViewById(R.id.tv_activity_desc)
        private val locationText: TextView = itemView.findViewById(R.id.tv_location)

        fun bind(activity: Activity) {
            timeText.text = activity.time
            titleText.text = activity.title
            descText.text = activity.description
            locationText.text = activity.location
        }
    }
}