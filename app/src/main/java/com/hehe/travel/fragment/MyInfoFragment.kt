package com.hehe.travel.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.hehe.travel.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class MyInfoFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var historyAdapter: TravelHistoryAdapter

    // Views
    private lateinit var tvGreeting: TextView
    private lateinit var tvDescription: TextView
    private lateinit var ivProfile: ImageView
    private lateinit var btnEditInfo: LinearLayout
    private lateinit var btnLogout: LinearLayout
    private lateinit var layoutEmptyState: ConstraintLayout
    private lateinit var rvTravelHistory: RecyclerView

    // 여행 기록 리스트
    private val allHistoryItems = mutableListOf<TravelHistoryItem>()
    private var historyLoaded = false
    private var planHistoryLoaded = false

    // 갤러리에서 이미지 선택 결과 처리
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadProfileImage(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        initGoogleClient()
        initViews(view)
        setupRecyclerView()
        loadUserProfile()
        loadAllHistory()
        setupClickListeners()
    }

    private fun initViews(view: View) {
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvDescription = view.findViewById(R.id.tvDescription)
        ivProfile = view.findViewById(R.id.ivProfile)
        btnEditInfo = view.findViewById(R.id.btnEditInfo)
        btnLogout = view.findViewById(R.id.btnLogout)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        rvTravelHistory = view.findViewById(R.id.rvTravelHistory)
    }

    private fun initGoogleClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
    }

    private fun setupRecyclerView() {
        historyAdapter = TravelHistoryAdapter(
            onItemClick = { item ->
                loadAndNavigateToPlan(item)
            },
            onDeleteClick = { item ->
                showDeleteConfirmDialog(item)
            },
            onPinClick = { item ->
                togglePin(item)
            }
        )
        rvTravelHistory.layoutManager = LinearLayoutManager(requireContext())
        rvTravelHistory.adapter = historyAdapter
    }

    private fun showDeleteConfirmDialog(item: TravelHistoryItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("여행 기록 삭제")
            .setMessage("${item.getCountryWithFlag()} 여행 기록을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                deleteHistoryItem(item)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteHistoryItem(item: TravelHistoryItem) {
        val uid = auth.currentUser?.uid ?: return

        val docRef = when (item.type) {
            3 -> Firebase.firestore.collection("planhistory").document(item.documentId)
            else -> Firebase.firestore.collection("history").document(uid)
                .collection("trips").document(item.documentId)
        }

        docRef.delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                allHistoryItems.removeAll { it.documentId == item.documentId }
                checkAndDisplayHistory()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun togglePin(item: TravelHistoryItem) {
        val uid = auth.currentUser?.uid ?: return
        val newPinState = !item.isPinned

        val docRef = when (item.type) {
            3 -> Firebase.firestore.collection("planhistory").document(item.documentId)
            else -> Firebase.firestore.collection("history").document(uid)
                .collection("trips").document(item.documentId)
        }

        docRef.update("isPinned", newPinState)
            .addOnSuccessListener {
                allHistoryItems.find { it.documentId == item.documentId }?.isPinned = newPinState
                Toast.makeText(requireContext(), if (newPinState) "고정되었습니다." else "고정 해제되었습니다.", Toast.LENGTH_SHORT).show()
                checkAndDisplayHistory()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "업데이트 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadAndNavigateToPlan(item: TravelHistoryItem) {
        val uid = auth.currentUser?.uid ?: return

        Toast.makeText(requireContext(), "일정을 불러오는 중...", Toast.LENGTH_SHORT).show()

        val docRef = when (item.type) {
            3 -> Firebase.firestore.collection("planhistory").document(item.documentId)
            else -> Firebase.firestore.collection("history").document(uid)
                .collection("trips").document(item.documentId)
        }

        docRef.get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(requireContext(), "데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val country = doc.getString("country") ?: ""
                val nickname = doc.getString("nickname") ?: ""
                val nights = doc.getLong("nights")?.toInt() ?: 0
                val startDate = doc.getString("startDate") ?: ""
                val endDate = doc.getString("endDate") ?: ""
                val travelStyle = doc.getString("travelStyle") ?: ""
                val keywords = doc.get("keywords") as? List<String> ?: emptyList()
                val hasSemiPass = doc.getBoolean("hasSemiPass") ?: false
                val budgetLabel = doc.getString("budgetLabel") ?: ""

                val days = doc.get("days") as? List<Map<String, Any>> ?: emptyList()

                if (days.isNotEmpty()) {
                    val daysJson = convertDaysToJson(days)
                    val intent = Intent(requireContext(), PlanActivity::class.java).apply {
                        putExtra("country", country)
                        putExtra("login", nickname)
                        putExtra("startDate", startDate)
                        putExtra("endDate", endDate)
                        putExtra("nights", nights)
                        putExtra("travelStyle", travelStyle)
                        putStringArrayListExtra("keywords", ArrayList(keywords))
                        putExtra("itineraryJson", daysJson)
                    }
                    startActivity(intent)
                } else {
                    val flight = doc.getString("flight") ?: ""
                    val accommodation = doc.getString("accommodation") ?: ""
                    val restaurant = doc.getString("restaurant") ?: ""

                    val intent = Intent(requireContext(), TravelResultDetailActivity::class.java).apply {
                        putExtra("country", country)
                        putExtra("nickname", nickname)
                        putExtra("flight", flight)
                        putExtra("accommodation", accommodation)
                        putExtra("restaurant", restaurant)
                        putExtra("hasSemiPass", hasSemiPass)
                        putExtra("budgetLabel", budgetLabel)
                    }
                    startActivity(intent)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun convertDaysToJson(days: List<Map<String, Any>>): String {
        val jsonArray = JSONArray()

        for (day in days) {
            val dayObj = JSONObject()
            dayObj.put("day", day["day"])
            dayObj.put("title", day["title"])

            val placesArray = JSONArray()
            val places = day["places"] as? List<Map<String, Any>> ?: emptyList()

            for (place in places) {
                val placeObj = JSONObject()
                placeObj.put("name", place["name"] ?: "")
                placeObj.put("description", place["description"] ?: "")
                placeObj.put("time", place["time"] ?: "")
                placeObj.put("duration", place["duration"] ?: "")
                placeObj.put("tip", place["tip"] ?: "")
                placesArray.put(placeObj)
            }

            dayObj.put("places", placesArray)
            jsonArray.put(dayObj)
        }

        return jsonArray.toString()
    }

    private fun loadAllHistory() {
        allHistoryItems.clear()
        historyLoaded = false
        planHistoryLoaded = false

        loadHistoryCollection()
        loadPlanHistoryCollection()
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return

        Firebase.firestore.collection("profiles").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nickname = document.getString("nickname") ?: "여행자"
                    tvGreeting.text = "$nickname 님"
                    tvDescription.text = "${nickname}님이 찾은 여행,\n한눈에 보기 쉽게 정리했어요!"

                    val profileImageUrl = document.getString("profileImageUrl")
                    if (!profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(profileImageUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_avatar)
                            .error(R.drawable.ic_avatar)
                            .into(ivProfile)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "프로필 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadProfileImage(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return

        Toast.makeText(requireContext(), "이미지 업로드 중...", Toast.LENGTH_SHORT).show()

        val storageRef = FirebaseStorage.getInstance().reference
        val profileImageRef = storageRef.child("profile_images/$uid.jpg")

        profileImageRef.putFile(uri)
            .addOnSuccessListener { _ ->
                profileImageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val data = mapOf("profileImageUrl" to downloadUrl.toString())
                    Firebase.firestore.collection("profiles").document(uid)
                        .set(data, SetOptions.merge())
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "프로필 이미지가 변경되었습니다!", Toast.LENGTH_SHORT).show()
                            Glide.with(this)
                                .load(downloadUrl)
                                .circleCrop()
                                .into(ivProfile)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "업로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadHistoryCollection() {
        val uid = auth.currentUser?.uid ?: return

        Firebase.firestore.collection("history")
            .document(uid)
            .collection("trips")
            .get()
            .addOnSuccessListener { documents ->
                Log.d("MyInfo", "History trips found: ${documents.size()}")

                for (doc in documents) {
                    val savedAt = doc.getTimestamp("createdAt")?.toDate() ?: continue

                    val hasSemiPass = doc.getBoolean("hasSemiPass") ?: false
                    val country = doc.getString("country") ?: ""
                    val nights = doc.getLong("nights")?.toInt() ?: 0
                    val daysCount = doc.getLong("daysCount")?.toInt() ?: 0
                    val startDate = doc.getString("startDate") ?: ""
                    val endDate = doc.getString("endDate") ?: ""
                    val travelStyle = doc.getString("travelStyle") ?: ""
                    val budgetLabel = doc.getString("budgetLabel") ?: ""
                    val isPinned = doc.getBoolean("isPinned") ?: false

                    val type = if (hasSemiPass) 2 else 1

                    allHistoryItems.add(
                        TravelHistoryItem(
                            documentId = doc.id,
                            country = country,
                            nights = nights,
                            daysCount = daysCount,
                            startDate = startDate,
                            endDate = endDate,
                            travelStyle = travelStyle,
                            hasSemiPass = hasSemiPass,
                            type = type,
                            savedAt = savedAt,
                            budgetLabel = budgetLabel,
                            isPinned = isPinned
                        )
                    )
                }

                historyLoaded = true
                checkAndDisplayHistory()
            }
            .addOnFailureListener { e ->
                Log.e("MyInfo", "History 로드 실패: ${e.message}")
                historyLoaded = true
                checkAndDisplayHistory()
            }
    }

    private fun loadPlanHistoryCollection() {
        val uid = auth.currentUser?.uid ?: return

        Firebase.firestore.collection("planhistory")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("MyInfo", "PlanHistory found: ${documents.size()}")

                for (doc in documents) {
                    val country = doc.getString("country") ?: ""
                    val nights = doc.getLong("nights")?.toInt() ?: 0
                    val daysCount = doc.getLong("daysCount")?.toInt() ?: 0
                    val startDate = doc.getString("startDate") ?: ""
                    val endDate = doc.getString("endDate") ?: ""
                    val travelStyle = doc.getString("travelStyle") ?: ""
                    val savedAt = doc.getTimestamp("savedAt")?.toDate()
                    val isPinned = doc.getBoolean("isPinned") ?: false

                    allHistoryItems.add(
                        TravelHistoryItem(
                            documentId = doc.id,
                            country = country,
                            nights = nights,
                            daysCount = daysCount,
                            startDate = startDate,
                            endDate = endDate,
                            travelStyle = travelStyle,
                            hasSemiPass = true,
                            type = 3,
                            savedAt = savedAt,
                            isPinned = isPinned
                        )
                    )
                }

                planHistoryLoaded = true
                checkAndDisplayHistory()
            }
            .addOnFailureListener { e ->
                Log.e("MyInfo", "PlanHistory 로드 실패: ${e.message}")
                planHistoryLoaded = true
                checkAndDisplayHistory()
            }
    }

    private fun checkAndDisplayHistory() {
        if (!historyLoaded || !planHistoryLoaded) return

        if (allHistoryItems.isEmpty()) {
            layoutEmptyState.visibility = View.VISIBLE
            tvDescription.visibility = View.GONE
            rvTravelHistory.visibility = View.GONE
        } else {
            val sorted = allHistoryItems.sortedWith(
                compareByDescending<TravelHistoryItem> { it.isPinned }
                    .thenByDescending { it.savedAt?.time ?: 0L }
            )
            historyAdapter.submitList(sorted)

            layoutEmptyState.visibility = View.GONE
            tvDescription.visibility = View.VISIBLE
            rvTravelHistory.visibility = View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        btnEditInfo.setOnClickListener {
            val intent = Intent(requireContext(), SammyPassMainActivity::class.java)
            intent.putExtra("isEditMode", true)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            signOut()
        }

        ivProfile.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }

    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener {
            auth.signOut()
            goToSearchAsGuest()
        }.addOnFailureListener {
            auth.signOut()
            goToSearchAsGuest()
        }
    }

    private fun goToSearchAsGuest() {
        GuestProfileData.clear()

        // MainContainerActivity로 이동 (나라검색 탭)
        val intent = Intent(requireContext(), MainContainerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra("isGuest", true)
        intent.putExtra("initialTab", R.id.tab_country)
        startActivity(intent)
        activity?.finish()
    }
}
