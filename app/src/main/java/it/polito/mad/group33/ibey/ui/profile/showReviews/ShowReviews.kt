package it.polito.mad.group33.ibey.ui.profile.showReviews

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.ChangeEventListener
import com.firebase.ui.firestore.FirestoreArray
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import it.polito.mad.group33.ibey.*
import it.polito.mad.group33.ibey.R
import it.polito.mad.group33.ibey.repository.firebaseRepository
import it.polito.mad.group33.ibey.ui.profile.UserViewModel
import kotlinx.android.synthetic.main.profile_show_rate.*
import kotlinx.android.synthetic.main.review_recycle_card.rating_bar_review
import kotlinx.android.synthetic.main.show_reviews_fragment.*


class ShowReviews : Fragment() {
    private lateinit var viewModel: ShowReviewsViewModel
    lateinit var userViewModel: UserViewModel
    lateinit var recyclerView:RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.show_reviews_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).populateToolbar(view.findViewById(R.id.toolbar))
        (activity as MainActivity).lockSwipeDrawer()
    }

    private fun setUserRate(rateCount:Long) {
            val rating:Float = (viewModel.totStelline/(rateCount+1))
            rating_bar_review.rating = rating
            if(rateCount==1L)
                text_review.text = requireContext().resources.getString(R.string.number_review_single,rateCount,rating)
            else
                text_review.text = requireContext().resources.getString(R.string.number_review,rateCount,rating)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ShowReviewsViewModel::class.java)
        val bundle = this.arguments
        if (bundle != null) {
            viewModel.user = User(bundle.getString(singleUserKey)!!)
            viewModel.firestoreArray=FirestoreArray<ShowReviewsViewModel.ReviewWUsers>(firebaseRepository.loadUserReviews(viewModel.user.uid),ShowReviewsViewModel.ReviewParser())
        } else {
            Log.d("debug_boundle","ShowReviews with no boundle")
            findNavController().popBackStack()
        }


        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)

        recyclerView = reviewListRecyclerView
        recyclerView.setHasFixedSize(true)
        var spanCount = 1
        if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            spanCount = 2

        recyclerView.layoutManager = GridLayoutManager(this.context, spanCount, RecyclerView.VERTICAL, false)
        recyclerView.adapter = AdapterReviewsList(viewModel.firestoreArray)
    }


    inner class AdapterReviewsList(var firestoreArray: FirestoreArray<ShowReviewsViewModel.ReviewWUsers>): RecyclerView.Adapter<AdapterReviewsList.ViewHolder>(),ChangeEventListener, LifecycleObserver{
        private val viewCard = 0
        private val viewLastCard = 1
        private val viewEmptyListCard = 2

        init {
            startListening()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun startListening() {
            if (!firestoreArray.isListening(this)) {
                firestoreArray.addChangeEventListener(this)
            }
        }

        /**
         * Stop listening for database changes and clear all items in the adapter.
         */
        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun stopListening() {
            firestoreArray.removeChangeEventListener(this)
            notifyDataSetChanged()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun cleanup(source: LifecycleOwner) {
            source.lifecycle.removeObserver(this)
        }



        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            var l = View(parent.context)
            var lastView = false
            when(viewType)
            {
                viewCard -> l = LayoutInflater.from(parent.context).inflate(R.layout.review_recycle_card, parent, false)
                viewEmptyListCard ->{
                    l = LayoutInflater.from(parent.context).inflate(R.layout.item_last_recycle_card, parent, false)
                    l.findViewById<TextView>(R.id.idEmptyListText).text= parentFragment!!.requireContext().resources.getString(R.string.noReviewString)
                    lastView = true
                }
            }
            return ViewHolder(l, lastView)
        }

        override fun getItemCount(): Int = firestoreArray.size

        override fun getItemViewType(position: Int): Int {
            return when {
                itemCount==0 -> viewEmptyListCard
                else -> viewCard
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int){
                holder.loadReview(firestoreArray.getSnapshot(position))
        }


        override fun onChildChanged(type: ChangeEventType,snapshot: DocumentSnapshot, newIndex: Int,oldIndex: Int) {
            when (type) {
                ChangeEventType.ADDED -> notifyItemInserted(newIndex)
                ChangeEventType.CHANGED -> notifyItemChanged(newIndex)
                ChangeEventType.REMOVED -> notifyItemRemoved(oldIndex)
                ChangeEventType.MOVED -> notifyItemMoved(oldIndex, newIndex)
                else -> throw IllegalStateException("Incomplete case statement")
            }
        }
        override fun onDataChanged() {}

        override fun onError(e: FirebaseFirestoreException){
            Log.e("FirestoreException",""+e.message)
        }


        inner class ViewHolder(val v: View, lastView: Boolean = false): RecyclerView.ViewHolder(v) {
            private val ratingBar: RatingBar? = if(!lastView) v.findViewById(R.id.rating_bar_review) else null
            val comment: MaterialTextView? = if(!lastView) v.findViewById(R.id.review) else null
            private val author: MaterialTextView? = if(!lastView) v.findViewById(R.id.authorNickname) else null
            private val authorPic: ImageView? = if(!lastView) v.findViewById(R.id.author_pic) else null


            fun bind(review: ShowReviewsViewModel.ReviewWUsers,haveAuthor:Boolean){
                viewModel.totStelline+= review.rate.toFloat()
                setUserRate(itemCount.toLong())

                ratingBar!!.rating = review.rate.toFloat()
                comment!!.text = "\u201C"+review.description+"\u201D"
                val navListener = View.OnClickListener {
                    if(review.authorUid==userViewModel.user.value!!.uid){
                        NavHostFragment.findNavController(parentFragment!!).navigate(R.id.action_showReviews_to_OwnProfileFragment)
                    }else{
                        val bundle = bundleOf(singleUserKey to viewModel.authorList[review.authorUid].toString())
                        NavHostFragment.findNavController(parentFragment!!).navigate(R.id.action_showReviews_to_otherProfileFragment, bundle)
                    }
                }

                if(haveAuthor){
                    author!!.text = viewModel.authorList[review.authorUid]?.nickname
                    author.setOnClickListener(navListener)
                    authorPic!!.setOnClickListener(navListener)

                }else{
                    if(review.description==""){
                        v.layoutParams.height=0
                        v.layoutParams = ViewGroup.MarginLayoutParams(0,0)
                    }else{
                        author!!.text=context?.getString(R.string.anonymous)
                    }

                }

                if(haveAuthor)
                    authorPic?.let { imageSetup(review.authorUid, it) }
            }

            fun loadReview(reviewSnap: DocumentSnapshot) {
                var review = ShowReviewsViewModel.ReviewParser().parseSnapshot(reviewSnap)


                val author=viewModel.authorList[review.authorUid]
                if(author!=null){
                    bind(review,true)
                }
                else {
                    firebaseRepository.loadPublicUser(review.authorUid).get().addOnSuccessListener {
                        document ->
                        viewModel.authorList[review.authorUid] = getUserFromDocument(document)
                        // author downloaded
                        bind(review,true)

                    }.addOnFailureListener{
                        bind(review,false)
                        Log.d("failLoadUser","Fail retrieve author from DB "+it.message)
                    }
                }
            }



            private fun imageSetup(id:String, authorPicView: ImageView){
                val url = "profile_0_36x36.jpg"
                firebaseRepository.getImg(url, "Users/$id").addOnSuccessListener {
                        parentFragment?.let { parentFrag -> Glide.with(parentFrag).asDrawable().load(it).circleCrop().into(authorPicView) }
                    }.addOnFailureListener {
                    Log.d("imageSetup","imageSetup Chips: could not download thumbnail $url")
                }
            }
        }
    }
}