package com.hehe.travel.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.hehe.travel.DateRangeActivity
import com.hehe.travel.GuestProfileData
import com.hehe.travel.R

class SearchFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var isGuest: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        isGuest = auth.currentUser == null

        setupSearch(view)
        loadUserProfile(view)
    }

    private fun loadUserProfile(view: View) {
        val uid = auth.currentUser?.uid

        if (isGuest || uid == null) {
            val guestNickname = GuestProfileData.nickname.ifEmpty { "여행자" }
            view.findViewById<TextView>(R.id.name1).text = guestNickname
            view.findViewById<TextView>(R.id.travel).text = "어디로 떠나실래요?"
            return
        }

        Firebase.firestore.collection("profiles").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nickname = document.getString("nickname") ?: ""
                    view.findViewById<TextView>(R.id.name1).text = nickname
                }
            }
            .addOnFailureListener {
                view.findViewById<TextView>(R.id.name1).text =
                    auth.currentUser?.displayName ?: "여행자"
            }

        view.findViewById<TextView>(R.id.travel).text = "어디로 떠나실래요?"
    }

    private fun setupSearch(view: View) {
        val etSearch = view.findViewById<EditText>(R.id.etSearchCountry)
        val btnSearch = view.findViewById<ImageView>(R.id.btnSearchCountry)

        btnSearch.setOnClickListener {
            val country = etSearch.text?.toString().orEmpty().trim()
            if (country.isBlank()) {
                Toast.makeText(requireContext(), "나라 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            hideKeyboard()

            if (isGuest) {
                GuestProfileData.country = country
            }

            val intent = Intent(requireContext(), DateRangeActivity::class.java)
            intent.putExtra("country", country)
            intent.putExtra("isGuest", isGuest)
            intent.putExtra("flowType", "guest")
            startActivity(intent)
        }

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                btnSearch.performClick()
                true
            } else false
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        view?.windowToken?.let { imm.hideSoftInputFromWindow(it, 0) }
    }
}
